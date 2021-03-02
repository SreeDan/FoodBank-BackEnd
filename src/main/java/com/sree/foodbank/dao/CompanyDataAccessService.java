package com.sree.foodbank.dao;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sree.foodbank.exception.ApiRequestException;
import com.sree.foodbank.model.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.json.simple.JSONArray;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import org.json.simple.parser.*;
import org.json.simple.JSONObject;
import org.springframework.web.util.UriComponentsBuilder;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigDecimal;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;


@Repository("postgres")
public class CompanyDataAccessService implements CompanyDao {
    private final JdbcTemplate jdbcTemplate;

    String url;
    String user;
    String password;
    String googleKey;
    String googleGeocodeKey;
    String sendGridKey;
    String jwtSecret;
    String email;
    {
        try {
            File postgresObj = new File("path/to/postgres/secret");
            File googleObj = new File("path/to/google/secret");
            File sendGridObj = new File("path/to/sendgrid/secret");
            File jwtObj = new File("path/to/jwt/secret");
            File emailObj = new File("path/to/email/secret");
            Scanner postgresReader = new Scanner(postgresObj);
            Scanner googleReader = new Scanner(googleObj);
            Scanner sendGridReader = new Scanner(sendGridObj);
            Scanner jwtScanner = new Scanner(jwtObj);
            Scanner emailScanner = new Scanner(emailObj);
            url = postgresReader.nextLine();
            user = postgresReader.nextLine();
            password = postgresReader.nextLine();
            googleKey = googleReader.nextLine();
            googleGeocodeKey = googleReader.nextLine();
            sendGridKey = sendGridReader.nextLine();
            jwtSecret = jwtScanner.nextLine();
            email = emailScanner.nextLine();
            postgresReader.close();
            googleReader.close();
            sendGridReader.close();
            jwtScanner.close();
            emailScanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    @Autowired
    public CompanyDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal decodeToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(jwtSecret))
                    .parseClaimsJws(token).getBody();
            System.out.println(claims.getSubject());
            return new BigDecimal(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<CompanyInfo> dashboard(String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException {
        String sql;
        BigDecimal id;
        if (googleToken(token) != null) {
            sql = "SELECT * FROM company WHERE personid = " + googleToken(token);
            id = googleToken(token);
        }
        else {
            sql = "SELECT * FROM company WHERE personid = " + decodeToken(token);
            id = decodeToken(token);
        }
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            Integer DBId = (Integer) resultSet.getObject("id");
            String name = resultSet.getString("companyname");
            String userType = resultSet.getString("class");
            Object objectAddress = null;
            try {
                objectAddress = new JSONParser().parse(resultSet.getString("address"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            JSONObject parsedAddress = (JSONObject) objectAddress;

            Array neededFood = resultSet.getArray("neededfood");
            Object neededFoodJson = null;
            Array availableFood = resultSet.getArray("availablefood");
            Object availableFoodJson = null;

            if (neededFood != null) {
                try {
                    neededFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(neededFood));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (availableFood != null) {
                try {
                    availableFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(availableFood));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            JSONArray neededFoodParsed = (JSONArray) neededFoodJson;
            JSONArray availableFoodParsed = (JSONArray) availableFoodJson;

            String url = resultSet.getString("url");
            String phone = resultSet.getString("phone");

            byte[] imgBytes = resultSet.getBytes("image");
            String imageType = resultSet.getString("imagetype");
            String base64Encoded = "";
            if (imgBytes != null) {
                base64Encoded = imageType + Base64.getEncoder().encodeToString(imgBytes);
            }
            System.out.println(name + " " + url + " " + neededFoodParsed);
            String email = resultSet.getString("email");
            String[] creds = getUserAndDash(id);
            System.out.println("checkpoint");
            return new CompanyInfo(DBId, creds[0], creds[1], name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, "", "", "", "", userType, base64Encoded, email, 0.0, 0.0);
        });
    }

    public String[] getUserAndDash(BigDecimal id) throws SQLException {
        final String sql = "SELECT * FROM credentials WHERE id = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getString("username"));
                return new String[]{rs.getString("username"), rs.getString("password")};
            }
            return new String[] {"", ""};
        }
    }

    @Override
    public String login(Login login) throws SQLException {
        final String sql = "SELECT * FROM credentials WHERE username = ? AND password = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, login.getUsername());
            pst.setString(2, login.getPassword());
            ResultSet rs = pst.executeQuery();
            Instant now = Instant.now();
            byte[] secret = Base64.getDecoder().decode(jwtSecret);
            String id = getId(login.getUsername(), login.getPassword()).toString();
            String jwt = Jwts.builder()
                    .setSubject(id)
                    .setAudience("Food Pantry Pickup")
                    .setIssuedAt(Date.from(now))
                    .signWith(Keys.hmacShaKeyFor(secret))
                    .compact();
            return jwt;
        }
    }

    public BigDecimal getId(String user, String password) throws SQLException {
        final String sql = "SELECT * FROM credentials WHERE username = ? AND password = ?";
        try (Connection con = DriverManager.getConnection(url, this.user, this.password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                return rs.getBigDecimal("id");
            return new BigDecimal(0);
        }
    }

    @Override
    public List<Food> getAllFood() {
        final String sql = "SELECT * from food ORDER BY foodname ASC";
        String[] listFood = {};
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            BigDecimal id = resultSet.getBigDecimal("foodid");
            String food = resultSet.getString("foodname");

            return new Food(id, food);
        });
    }

    @Override
    public CompanyReturn insertCompany(BigDecimal id, String token, Company company) throws GeneralSecurityException, IOException {
        token = company.getToken();
        id = googleToken(token);
        String[] food = {""};
        final String sql = "INSERT INTO company (personid, companyname, availablefood, address, class) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            String addressString = "{\"Zip\": null, \"State\": null, \"Street\": null, \"City\": null}";
            if (company.getAddress() != null) {
                addressString = String.valueOf(company.getAddress());
            }
            BigDecimal[] availableFood = {null};
            if (company.getUserType().equals("bank")) {
                availableFood = new BigDecimal[] {};
            }
            Array availableFoodArray = con.createArrayOf("DECIMAL", availableFood);

            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(addressString);

            pst.setBigDecimal(1, id);
            pst.setString(2, company.getName());
            pst.setArray(3, availableFoodArray);
            pst.setObject(4, jsonObject);
            pst.setString(5, company.getUserType());
            pst.executeUpdate();
            return new CompanyReturn(0, company.getName(), "", "", new String[]{"asd"}, new String[]{"das"}, jsonObject, "", "", "", 0.0, 0.0, 0.0);
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return new CompanyReturn(0, "asd", "", "", new String[]{"asd"}, new String[]{"das"}, new Object(), "", "", "", 0.0, 0.0, 0.0);
    }

    @Override
    public int insertFood(BigDecimal id, String token, CompanyFood companyFood) throws GeneralSecurityException, IOException {
        token = companyFood.getToken();
        if (googleToken(token) != null) {
            id = googleToken(token);
        }
        else {
            id = decodeToken(token);
        }
        id = googleToken(token);
        final String sql = "UPDATE company SET neededfood = ? WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            BigDecimal[] foodIds = foodConvertNametoId(companyFood.getNeededFood());

            Array foodArray = con.createArrayOf("DECIMAL", foodIds);
            pst.setArray(1, foodArray);
            pst.setBigDecimal(2, id);
            pst.executeUpdate();

        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return 1;
    }


    @Override
    public List<CompanyReturn> selectAllCompanies(CompanyReturn companyReturn) throws SQLException {
        //final String sql = "SELECT id, companyname, neededfood, availablefood, address, class, image, imagetype FROM company WHERE class = 'bank'";
        final String sql = "SELECT * FROM company WHERE class = 'bank'";
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            Integer DBId = (Integer) resultSet.getObject("id");
            String name = resultSet.getString("companyname");
            String userType = resultSet.getString("class");
            Object objectAddress = null;
            try {
                objectAddress = new JSONParser().parse(resultSet.getString("address"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            JSONObject parsedAddress = (JSONObject) objectAddress;

            Array neededFood = resultSet.getArray("neededfood");
            Object neededFoodJson = null;
            Array availableFood = resultSet.getArray("availablefood");
            Object availableFoodJson = null;

            if (neededFood != null) {
                try {
                    neededFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(neededFood));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (availableFood != null) {
                try {
                    availableFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(availableFood));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            JSONArray neededFoodParsed = (JSONArray) neededFoodJson;
            JSONArray availableFoodParsed = (JSONArray) availableFoodJson;

            String url = resultSet.getString("url");
            String phone = resultSet.getString("phone");

            byte[] imgBytes = resultSet.getBytes("image");
            String imageType = resultSet.getString("imagetype");
            String base64Encoded = "";
            if (imgBytes != null) {
                base64Encoded = imageType + Base64.getEncoder().encodeToString(imgBytes);
            }
            String email = resultSet.getString("email");
            return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded, email, 0.0, 0.0, 0.0);
            //return new CompanyReturn(DBId, name, url, phone, neededFoodNames, availableFoodNames, parsedAddress, userType);
        });
    }

    @Override
    public List<CompanyReturnFood> selectCompanyFood() {
        final String sql = "SELECT companyname, neededfood FROM company";
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            String name = resultSet.getString("companyname");
            Array food = resultSet.getArray("neededfood");
            BigDecimal[] foodString;
            String[] foodNames = {};

            if (food != null) {
                foodNames = foodConvertIdtoName(food);
            }
            return new CompanyReturnFood(name, foodNames);

        });
    }

    @Override
    public List<CompanyReturn> filterByFood(CompanyFilter companyFilter) throws SQLException {
        final String sql = "SELECT * FROM company WHERE class = 'bank'";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            String type = companyFilter.getType();
            if (type == null) {
                type = "";
            }
            String newSql = "SELECT * FROM company WHERE class = 'bank'";
            String[] availableFoodFilter;
            String[] neededFoodFilter;
            BigDecimal[] availableFoodIds;
            BigDecimal[] neededFoodIds;
            Array availableFoodIdArray;
            Array neededFoodIdArray;

            switch (type) {
                case "availableFood" -> {
                    availableFoodFilter = companyFilter.getAvailableFood();
                    availableFoodIds = foodConvertNametoId(availableFoodFilter);
                    availableFoodIdArray = con.createArrayOf("DECIMAL", availableFoodIds);
                    newSql = "SELECT * FROM company WHERE availablefood @> '" + availableFoodIdArray + "' AND class = 'bank'";
                }
                case "neededFood" -> {
                    neededFoodFilter = companyFilter.getNeededFood();
                    neededFoodIds = foodConvertNametoId(neededFoodFilter);
                    neededFoodIdArray = con.createArrayOf("DECIMAL", neededFoodIds);
                    newSql = "SELECT * FROM company WHERE neededfood @> '" + neededFoodIdArray + "' AND class = 'bank'";
                }
                case "both" -> {
                    neededFoodFilter = companyFilter.getNeededFood();
                    availableFoodFilter = companyFilter.getAvailableFood();
                    neededFoodIds = foodConvertNametoId(neededFoodFilter);
                    availableFoodIds = foodConvertNametoId(availableFoodFilter);
                    neededFoodIdArray = con.createArrayOf("DECIMAL", neededFoodIds);
                    availableFoodIdArray = con.createArrayOf("DECIMAL", availableFoodIds);
                    newSql = "SELECT * FROM company WHERE neededfood @> '" + neededFoodIdArray + "' AND availablefood @> '" + availableFoodIdArray + "' AND class = 'bank'";
                }
            }

            return jdbcTemplate.query(newSql, (resultSet, i) -> {
                Integer DBId = (Integer) resultSet.getObject("id");
                String name = resultSet.getString("companyname");
                String userType = resultSet.getString("class");
                Object objectAddress = null;
                try {
                    objectAddress = new JSONParser().parse(resultSet.getString("address"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                JSONObject parsedAddress = (JSONObject) objectAddress;

                Array neededFood = resultSet.getArray("neededfood");
                Object neededFoodJson = null;
                Array availableFood = resultSet.getArray("availablefood");
                Object availableFoodJson = null;

                if (neededFood != null) {
                    try {
                        neededFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(neededFood));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if (availableFood != null) {
                    try {
                        availableFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(availableFood));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                JSONArray neededFoodParsed = (JSONArray) neededFoodJson;
                JSONArray availableFoodParsed = (JSONArray) availableFoodJson;

                String url = resultSet.getString("url");
                String phone = resultSet.getString("phone");

                byte[] imgBytes = resultSet.getBytes("image");
                String imageType = resultSet.getString("imagetype");
                String base64Encoded = "";
                if (imgBytes != null) {
                    base64Encoded = imageType + Base64.getEncoder().encodeToString(imgBytes);
                }
                String email = resultSet.getString("email");
                return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded, email, 0.0, 0.0, 0.0);
                //return new CompanyReturn(DBId, name, url, phone, neededFoodNames, availableFoodNames, parsedAddress, userType);
            });

        }
    }


    @Override
    public List<CompanyReturn> selectCompanyById(BigDecimal id, Double distance) {
        //final String sql = "SELECT id, companyname, neededfood, availablefood, address, class, image, imagetype FROM company WHERE id = " + id;
        final String sql = "SELECT * FROM company WHERE id = " + id;
        return jdbcTemplate.query(sql, (resultSet, i) -> {

            Integer DBId = (Integer) resultSet.getObject("id");
            String name = resultSet.getString("companyname");
            String userType = resultSet.getString("class");
            Object objectAddress = null;
            try {
                objectAddress = new JSONParser().parse(resultSet.getString("address"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            JSONObject parsedAddress = (JSONObject) objectAddress;

            Array neededFood = resultSet.getArray("neededfood");
            Object neededFoodJson = null;
            Array availableFood = resultSet.getArray("availablefood");
            Object availableFoodJson = null;

            if (neededFood != null) {
                try {
                    neededFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(neededFood));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (availableFood != null) {
                try {
                    availableFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(availableFood));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            JSONArray neededFoodParsed = (JSONArray) neededFoodJson;
            JSONArray availableFoodParsed = (JSONArray) availableFoodJson;

            String url = resultSet.getString("url");
            String phone = resultSet.getString("phone");

            byte[] imgBytes = new byte[0];
            String imageType = "";
            if (resultSet.next()) {
                imgBytes = resultSet.getBytes("image");
                imageType = resultSet.getString("imagetype");
            }
            String email = resultSet.getString("email");

            String base64Encoded = imageType + Base64.getEncoder().encodeToString(imgBytes);

            return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded, email, distance,  0.0, 0.0);
        });
    }

    @Override
    public List<CompanyNeededFood> getCompanyNeededFoodById(BigDecimal id) {
        final String sql = "SELECT * FROM company WHERE id = " + id;
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            Array neededFood = resultSet.getArray("neededFood");
            String[] neededFoodNames = {};
            if (neededFood != null) {
                neededFoodNames = foodConvertIdtoName(neededFood);
            }
            return new CompanyNeededFood(neededFoodNames);
        });
    }

    @Override
    public int deleteCompanybyId(BigDecimal id) {
        return 0;
    }

    @Override
    public int deleteFood(BigDecimal id, String token) throws GeneralSecurityException, IOException {
        if (googleToken(token) != null) {
            id = googleToken(token);
        }
        else {
            id = decodeToken(token);
        }
        final String sql = "DELETE FROM companyfood WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            pst.executeUpdate();
            return 1;
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return 0;
        }
    }

    @Override
    public int updateCompanybyId(String token, CompanyInfo companyUpdate) throws GeneralSecurityException, IOException, InterruptedException {
        BigDecimal id;
        if (googleToken(token) != null) {
            id = googleToken(token);
        }
        else {
            id = decodeToken(token);
        }

        final String sql = "UPDATE company SET address = ?::JSON, url = ?, email = ?, phone = ?, image = ?, imagetype = ?, lat = ?, long = ?, place_id = ? WHERE personid = ?";
        String user = companyUpdate.getUser();
        String pass = companyUpdate.getPassword();
        String billing = companyUpdate.getBilling();
        String city = companyUpdate.getCity();
        String state = companyUpdate.getState();
        String ZIP = companyUpdate.getZIP();

        String[] latLng = getLatLng(billing, city, state, ZIP);
        if (latLng[0] == null) {
            if (latLng[3] == null) {
                throw new ApiRequestException("Error Validating Your Address - Please Try Again Later");
            }
            throw new ApiRequestException("Invalid Address");
        }

        String url = companyUpdate.getUrl();
        String phone = companyUpdate.getPhone();
        String email = companyUpdate.getEmail();
        String image = companyUpdate.getImage();


        String[] address;
        if (billing == null || city == null || state == null || state == null) {
            address = new String[] {"{\"ZIP\": \"null\", \"City\": \"null\", \"State\": \"null\", \"Street\": \"null\"}"};
        } else {
            address = new String[] {"{\"ZIP\": \"" + ZIP + "\", \"City\": \"" + city + "\", \"State\": \"" + state + "\", \"Street\": \"" + billing + "\"}"};
        }
        if (image.isEmpty()) {
            updateWithoutImage(user, pass, address, url, email, phone, latLng[0], latLng[1], latLng[2], id);
            return 1;
        }

        String[] strings = image.split(",");
        String extension;
        String fullExtension;
        switch (strings[0]) {
            case "data:image/jpeg;base64" -> {
                extension = "jpeg";
                fullExtension = "data:image/jpeg;base64,";
            }
            case "data:image/png;base64" -> {
                extension = "png";
                fullExtension = "data:image/png;base64,";
            }
            default -> {
                extension = "jpg";
                fullExtension = "data:image/jpg;base64,";
            }
        }
        byte[] data = DatatypeConverter.parseBase64Binary(strings[1]);
        String path = "/path/to/image/folder/test_image." + extension;
        File file = new File(path);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File newFile = new File(path);
        FileInputStream fis = new FileInputStream(file);

        updateUserPass(user, pass, id);
        try (Connection con = DriverManager.getConnection(this.url, this.user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            Array availableFoodArray = con.createArrayOf("DECIMAL", new BigDecimal[] {});
            pst.setObject(1, address[0]);
            pst.setString(2, url);
            pst.setString(3, email);
            pst.setString(4, phone);
            pst.setBinaryStream(5, fis, newFile.length());
            pst.setString(6, fullExtension);
            pst.setString(7, latLng[0]);
            pst.setString(8, latLng[1]);
            pst.setString(9, latLng[2]);
            pst.setBigDecimal(10, id);
            pst.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return 0;
    }

    public void updateWithoutImage(String user, String pass, String[] address, String url, String email, String phone, String lat, String lng, String place_id, BigDecimal id) {
        String sql = "UPDATE company SET address = ?::JSON, url = ?, email = ?, phone = ?, lat = ?, long = ?, place_id = ? WHERE personid = ?";
        updateUserPass(user, pass, id);
        try (Connection con = DriverManager.getConnection(this.url, this.user, this.password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setObject(1, address[0]);
            pst.setString(2, url);
            pst.setString(3, email);
            pst.setString(4, phone);
            pst.setString(5, lat);
            pst.setString(6, lng);
            pst.setString(7, place_id);
            pst.setBigDecimal(8, id);
            pst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public int updateFood(String token, List<Food> foodUpdate) throws GeneralSecurityException, IOException, SQLException {
        BigDecimal id;
        if (googleToken(token) != null) {
            id = googleToken(token);
        }
        else {
            id = decodeToken(token);
        }

        BigDecimal[] updatedFood = new BigDecimal[foodUpdate.size()];
        for (int x = 0; x < foodUpdate.size(); x++) {
            updatedFood[x] = foodUpdate.get(x).getId();
        }
        final String sql = "UPDATE company SET availablefood = ? WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            Array food = con.createArrayOf("DECIMAL", updatedFood);
            pst.setArray(1, food);
            pst.setBigDecimal(2, id);
            pst.executeUpdate();
        }
        return 1;
    }

    @Override
    public int requestFood(String token, CompanyRequest companyRequest) throws SQLException, IOException, GeneralSecurityException, MessagingException {
        //final String sql = "INSERT INTO requests (requesterid, receiverid, food, datetime, type, status) VALUES (?, ?, ?, ?, ?, ?)";
        final String sql = "INSERT INTO requests (requesterid, receiverid, food, type, status, show) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            BigDecimal requesterId;
            if (googleToken(token) != null) {
                requesterId = googleToken(token);
            }
            else {
                requesterId = decodeToken(token);
            }
            BigDecimal receiverId = getId(companyRequest.getReceiverId());
            BigDecimal[] foodIds = foodConvertNametoId(companyRequest.getFood());
            Array foodArray = con.createArrayOf("DECIMAL", foodIds);

            pst.setBigDecimal(1, requesterId);
            pst.setBigDecimal(2, receiverId);
            pst.setArray(3, foodArray);
            //pst.setString(4, companyRequest.getDate());
            pst.setString(4, companyRequest.getType());
            pst.setString(5, companyRequest.getStatus());
            pst.setBoolean(6, true);
            pst.executeUpdate();
        }
        return 1;
    }

    @Override
    public int updateRequest(CompanyRequest companyRequest) throws SQLException, GeneralSecurityException, IOException {
        final String sql = "UPDATE requests SET status = ?, datetime = ? WHERE requestId = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            Date date = companyRequest.getDate();
            String dateString = date.toString();
            System.out.println(companyRequest.getDate());
            pst.setString(1, companyRequest.getStatus());
            pst.setString(2, dateString);
            pst.setInt(3, companyRequest.getRequestId());
            pst.executeUpdate();
            if (companyRequest.getStatus().equals("accepted")) {
                String[] emailInfo = emailInfo(companyRequest.getRequestId());
                sendEmail(emailInfo, dateString);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public String[] emailInfo(int requestId) throws SQLException {
        final String sql = "SELECT * FROM requests WHERE requestid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, requestId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                BigDecimal requesterId = rs.getBigDecimal("requesterId");
                BigDecimal receiverId = rs.getBigDecimal("receiverId");
                String[] requesterInfo = info(requesterId);
                String[] receiverInfo = info(receiverId);
                return new String[]{requesterInfo[0], requesterInfo[1], receiverInfo[0], receiverInfo[2]};
            }
            return new String[0];
        }
    }

    public String[] info(BigDecimal id) throws SQLException {
        final String sql = "SELECT * FROM company WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                return new String[]{rs.getString("companyname"), rs.getString("email"), rs.getString("address")};
            return new String[3];
        }
    }



    @Override
    public List<CompanyReturnRequest> getRequest(String token) throws SQLException, GeneralSecurityException, IOException {
        BigDecimal id;
        if (googleToken(token) != null) {
            id = googleToken(token);
        }
        else {
            id = decodeToken(token);
        }
        final String sql = "SELECT requestid, requesterid, receiverid, food, datetime, type, status FROM requests WHERE requesterid = " + id + " OR receiverid = " + id;
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            int requestId = resultSet.getInt("requestid");
            BigDecimal requesterId = resultSet.getBigDecimal("requesterid");
            BigDecimal receiverId = resultSet.getBigDecimal("receiverid");
            Array food = resultSet.getArray("food");
            String date = resultSet.getString("datetime");
            String type = resultSet.getString("type");
            String status = resultSet.getString("status");
            String requesterName = getName(requesterId);
            String receiverName = getName(receiverId);
            String[] foodNames = {};

            if (food != null) {
                foodNames = foodConvertIdtoName(food);
            }

            if (date == null) {
                date = "No time given";
            }
            return new CompanyReturnRequest("", requestId, requesterName, receiverName, foodNames, date, type, status);
        });
    }

    @Override
    public int addImage(ImageEncode imageEncode) throws FileNotFoundException, SQLException {
        System.out.println(imageEncode.getBase64());
        String[] strings = imageEncode.getBase64().split(",");
        String extension;
        String fullExtension;
        //check image's extension
        switch (strings[0]) {
            case "data:image/jpeg;base64" -> {
                extension = "jpeg";
                fullExtension = "data:image/jpeg;base64,";
            }
            case "data:image/png;base64" -> {
                extension = "png";
                fullExtension = "data:image/png;base64,";
            }
            default -> {
                extension = "jpg";
                fullExtension = "data:image/jpg;base64,";
            }
        }
        byte[] data = DatatypeConverter.parseBase64Binary(strings[1]);
        String path = "path/to/images/image." + extension;
        File file = new File(path);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File newFile = new File(path);
        FileInputStream fis = new FileInputStream(file);

        final String sql = "UPDATE company SET image = ?, imagetype = ? WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBinaryStream(1, fis, newFile.length());
            pst.setString(2, fullExtension);
            pst.setBigDecimal(3, imageEncode.getId());
            BigDecimal thing = googleToken(imageEncode.getToken());
            //pst.setBigDecimal(3, googleToken(imageEncode.getToken()));
            pst.execute();
            fis.close();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public String getName(BigDecimal id) throws SQLException {
        final String sql = "SELECT companyname FROM company WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            ResultSet rs = pst.executeQuery();
            String name = "";
            while (rs.next()) {
                name = rs.getString("companyname");
            }
            return name;
        }
    }

    public BigDecimal getId(int id) throws SQLException {
        final String sql = "SELECT personid FROM company WHERE id = ?";
        try(Connection con = DriverManager.getConnection(url, user, password);
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            BigDecimal personId = BigDecimal.valueOf(0);
            while (rs.next()) {
                personId = rs.getBigDecimal("personid");
            }
            return personId;
        }
    }

    @Override
    public String foodIdToName(BigDecimal foodId) throws SQLException {
        final String sql = "SELECT foodname FROM food WHERE foodid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pstGet = con.prepareStatement(sql)) {
            pstGet.setBigDecimal(1, foodId);
            ResultSet rs = pstGet.executeQuery();
            if (rs.next()) {
                String food = rs.getString("foodname");
                return food;
            }
            return "ERROR";
        }

    }

    @Override
    public BigDecimal foodNameToId(String foodName) throws SQLException {
        final String sql = "SELECT foodid FROM food WHERE foodname = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, foodName);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                BigDecimal id = rs.getBigDecimal("foodid");
                return id;
            }
            return BigDecimal.valueOf(0);
        }
    }

    @Override
    public BigDecimal googleToken(String token) throws GeneralSecurityException, IOException {
        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        final JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(googleKey))
                .build();
        System.out.println(token);
        GoogleIdToken idToken = verifier.verify(token);
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            String userId = payload.getSubject();
            return new BigDecimal(userId);
        } else {
            return new BigDecimal("null");
        }
    }

    @Override
    public void sendEmail(String[] info, String date) throws IOException, ParseException {
        Email from = new Email(email);
        String subject = "Request has been approved, check timings";
        System.out.println(info[1]);
        Email to = new Email(info[1]);
        JSONParser parser = new JSONParser();
        JsonObject obj = new JsonParser().parse(info[3]).getAsJsonObject();
        String address = String.join(", ", obj.get("Street").getAsString(), obj.get("City").getAsString(), obj.get("State").getAsString() + " " + obj.get("Zip").getAsString());
        Content content = new Content("text/plain", "Dear " + info[0] + ", Your request to pickup food was accepted by " + info[2] +
                ". You can pick it up at " + address + " on " + date);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
        } catch (IOException ex) {
            throw ex;
        }
    }

    @Override
    public boolean createAccount(CreateAccount createAccount) throws SQLException {
        final String sql = "INSERT INTO credentials (username, password, id) VALUES (?, ?, ?)";
        double min = 100000000000000000000.0;
        double max = 999999999999999999999.0;
        Random random = new Random();
        double randomValue = min + (max - min) * random.nextDouble();
        BigDecimal id = new BigDecimal(randomValue);


        System.out.println("billing " + createAccount.getBilling());
        boolean status = addCredentials(id, createAccount.getName(), createAccount);
        if (status) {
            try (Connection con = DriverManager.getConnection(url, user, password);
                 PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, createAccount.getUsername());
                pst.setString(2, createAccount.getPassword());
                pst.setBigDecimal(3, id);
                pst.executeUpdate();
                return true;
            }
        }
        return false;
    }

    @Override
    public List<CompanyReturn> locationFiltering(Location location) throws SQLException, IOException, InterruptedException {
        String sql = "SELECT * FROM company WHERE class = 'bank' AND address ->> 'State' = ?";
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ArrayList<String> place_ids = new ArrayList<String>();
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, location.getState());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("id"));
                place_ids.add(rs.getString("place_id"));
            }
        }
        int placeholder = 0;
        String[][] place_ids_array = new String[1 + (place_ids.size() / 25)][25];
        int[][] ids_2d = new int[1 + (ids.size() / 25)][25];
        for (int rows = 0; rows < place_ids_array.length; rows++) {
            for (int cols = 0; cols < place_ids_array[rows].length; cols++) {
                if (placeholder < place_ids.size()) {
                    place_ids_array[rows][cols] = place_ids.get(placeholder);
                    ids_2d[rows][cols] = ids.get(placeholder);
                    placeholder++;
                }
            }
        }

        Double[][] distance = new Double[1 + (place_ids.size() / 25)][25];
        int num_distances = 0;
        System.out.println(Arrays.toString(place_ids_array[0]));
        //Creating 2d array of 25 locations each beacuse you can only send 25 locations at once to the Google API
        for (int rows = 0; rows < distance.length; rows++) {
            String origin = location.getLat().toString() + "," + location.getLng().toString();
            StringBuilder destinations = new StringBuilder();
            for (int cols = 0; cols < distance[rows].length; cols++) {
                String place_id = place_ids_array[rows][cols];
                if (place_id != null) {
                    destinations.append("place_id:").append(place_id).append("|");
                }
            }
            final String POSTS_API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial" + origin + destinations.substring(0, destinations.length() - 3) + "&key=" + URLEncoder.encode(googleGeocodeKey, "UTF-8");

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial")
                    .queryParam("origins", origin)
                    .queryParam("destinations", destinations)
                    .queryParam("key", googleGeocodeKey)
                    .build()
                    .encode()
                    .toUriString();

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .header("accept", "application/json")
                    .uri(URI.create(url))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new ApiRequestException("Can't use this service at the moment");
            } catch (InterruptedException e) {
                throw new ApiRequestException("Can't use this service at the moment");
            }
            String body = response.body();
            org.json.JSONObject obj = new org.json.JSONObject(body);
            org.json.JSONArray res = obj.getJSONArray("rows").getJSONObject(0).getJSONArray("elements");

            for (int cols = 0; cols < distance[rows].length; cols++) {
                if (place_ids_array[rows][cols] != null) {
                    String distanceText = res.getJSONObject(cols).getJSONObject("distance").getString("text");
                    distanceText = distanceText.replace(",", "");
                    distance[rows][cols] = Double.valueOf(distanceText.substring(0, distanceText.indexOf(" ")));
                    num_distances++;
                }
            }
        }

        Distance[] single_distances = new Distance[num_distances];
        placeholder = 0;
        outerloop:
        for (int rows = 0; rows < distance.length; rows++) {
            for (int cols = 0; cols < distance[rows].length; cols++) {
                single_distances[placeholder] = new Distance(ids_2d[rows][cols], distance[rows][cols]);
                placeholder++;
                if (placeholder == num_distances) {
                    break outerloop;
                }
            }
        }
        Distance[] sorted = mergeSort(single_distances);
        ArrayList<CompanyReturn> companyReturns = new ArrayList<CompanyReturn>();
        List<CompanyReturn> companyReturnList = new ArrayList<CompanyReturn>();
        for (Distance d: sorted) {
            companyReturnList.add(selectCompanyById(BigDecimal.valueOf(d.getId()), d.getDistance()).get(0));
        }

        return companyReturnList;
    }

    public Distance[] mergeSort(Distance[] array) {
        if(array.length <= 1) {
            return array;
        }

        int midpoint = array.length / 2;
        Distance[] left = new Distance[midpoint];
        Distance[] right;
        if(array.length % 2 == 0) {
            right = new Distance[midpoint];
        } else {
            right = new Distance[midpoint + 1];
        }

        for(int i=0; i < midpoint; i++) {
            left[i] = array[i];
        }

        for(int j=0; j < right.length; j++) {
            right[j] = array[midpoint+j];
        }

        Distance[] result = new Distance[array.length];
        left = mergeSort(left);
        right = mergeSort(right);
        result = merge(left, right);
        return result;
    }

    public Distance[] merge(Distance[] left, Distance[] right) {
        Distance[] result = new Distance[left.length + right.length];
        int leftPointer, rightPointer, resultPointer;
        leftPointer = rightPointer = resultPointer = 0;
        while(leftPointer < left.length || rightPointer < right.length) {
            if(leftPointer < left.length && rightPointer < right.length) {
                if(left[leftPointer].getDistance() < right[rightPointer].getDistance()) {
                    result[resultPointer++] = left[leftPointer++];
                } else {
                    result[resultPointer++] = right[rightPointer++];
                }

            }
            else if(leftPointer < left.length) {
                result[resultPointer++] = left[leftPointer++];
            }
            else if(rightPointer < right.length) {
                result[resultPointer++] = right[rightPointer++];
            }
        }
        return result;
    }

    public boolean addCredentials(BigDecimal id, String name, CreateAccount createAccount) throws SQLException {
        final String sql = "INSERT INTO company (personid, companyname, email, class, address) VALUES (?, ?, ?, ?, ?::JSON)";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {

            String billing = createAccount.getBilling();
            String city = createAccount.getCity();
            String state = createAccount.getState();
            String ZIP = createAccount.getZIP();

            String[] latLng = getLatLng(billing, city, state, ZIP);
            if (latLng[0] == null) {
                if (latLng[3] == null) {
                    throw new ApiRequestException("Error Creating an Account at this Time");
                }
                throw new ApiRequestException("Invalid Address");
            }

            String[] address;
            if (createAccount.getBilling() == null || createAccount.getCity() == null || createAccount.getState() == null || createAccount.getZIP() == null) {
                address = new String[] {"{\"ZIP\": \"null\", \"City\": \"null\", \"State\": \"null\", \"Street\": \"null\"}"};
            } else {
                address = new String[] {"{\"ZIP\": \"" + ZIP + "\", \"City\": \"" + city + "\", \"State\": \"" + state + "\", \"Street\": \"" + billing + "\"}"};
            }
            System.out.println(createAccount.getType());
            pst.setBigDecimal(1, id);
            pst.setString(2, name);
            pst.setString(3, createAccount.getEmail());
            pst.setString(4, createAccount.getType());
            pst.setObject(5, address[0]);
            pst.executeUpdate();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String[] getLatLng(String billing, String city, String state, String ZIP) throws IOException, InterruptedException {
        billing = billing.replace(' ', '+');
        city = city.replace(' ', '+');
        final String POSTS_API_URL = "https://maps.googleapis.com/maps/api/geocode/json?address=" + billing + ",+" + city + ",+" + state + "&key=" + googleGeocodeKey;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("accept", "application/json")
                .uri(URI.create(POSTS_API_URL))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String s = response.body();
        org.json.JSONObject obj = new org.json.JSONObject(s);

        String[] values = new String[3];
        try {
            if (obj.getString("status").equals("ZERO_RESULTS")) {
                values[0] = null;
                values[1] = null;
                values[2] = null;
                values[2] = "0.0";
                return values;
                //throw new ApiRequestException("Invalid Address");
            }
        } catch (Exception e) {}
        org.json.JSONObject res = obj.getJSONArray("results").getJSONObject(0);
        org.json.JSONObject loc = res.getJSONObject("geometry").getJSONObject("location");
        try {
            if (res.getBoolean("partial_match")) {
                values[0] = null;
                values[1] = null;
                values[2] = null;
                values[3] = "0.0";
                return values;
                //throw new ApiRequestException("Invalid Address");
            }
        } catch (Exception e) {
            if (res.length() != 0) {
                values[0] = Double.toString(loc.getDouble("lat"));
                values[1] = Double.toString(loc.getDouble("lng"));
                values[2] = res.getString("place_id");
                return values;
            }
        }
        values[0] = null;
        values[1] = null;
        values[2] = null;
        values[3] = null;
        return values;
    }

    public BigDecimal[] getFoodFromId(BigDecimal id) throws SQLException {
        final String sqlGet = "SELECT neededfood FROM company WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pstGet = con.prepareStatement(sqlGet)) {
            pstGet.setBigDecimal(1, id);
            ResultSet rs = pstGet.executeQuery();
            if (rs.next()) {
                Array food = rs.getArray("neededfood");
                return (BigDecimal[]) food.getArray();
            }
            return new BigDecimal[0];
        }

    }

    public String[] foodConvertIdtoName(Array food) throws SQLException {
        BigDecimal[] foodString = (BigDecimal[]) food.getArray();
        String[] foodNames = new String[foodString.length];
        for (int x = 0; x < foodString.length; x++) {
            String temp = foodIdToName(foodString[x]);
            foodNames[x] = temp;
        }
        return foodNames;
    }

    public BigDecimal[] foodConvertNametoId(String[] foodNames) throws SQLException {
        BigDecimal[] foodIds = new BigDecimal[foodNames.length];
        for (int x = 0; x < foodNames.length; x++) {
            BigDecimal temp = foodNameToId(foodNames[x]);
            foodIds[x] = temp;
        }
        return foodIds;
    }

    public String[] foodConvertIdToNameBeforeJson(BigDecimal[] foodId) throws SQLException {
        String[] foodString = new String[foodId.length];
        for (int x = 0; x < foodId.length; x++) {
            foodString[x] = foodIdToName(foodId[x]);
        }
        Arrays.sort(foodString);
        return foodString;
    }

    public String foodConvertIdtoNameJson(Array food) throws SQLException {
        BigDecimal[] foodId = (BigDecimal[]) food.getArray();
        StringBuilder tempString = new StringBuilder();
        String[] foodName = foodConvertIdToNameBeforeJson(foodId);
        for (int x = 0; x < foodId.length; x++) {
            tempString.append(", {\"value\": \"").append(foodNameToId(foodName[x])).append("\", \"label\": \"").append(foodName[x]).append("\"}");
        }
        String newThing = "";
        newThing += "[" + tempString.substring(2) + "]";
        return newThing;
    }

    public void updateUserPass(String user, String password, BigDecimal id)  {
        final String sql = "UPDATE credentials SET username = ?, password = ? WHERE id = ?";
        try (Connection con = DriverManager.getConnection(url, this.user, this.password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            pst.setString(2, password);
            pst.setBigDecimal(3, id);
            pst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    final RateLimiter rateLimiter = RateLimiter.create(10.0);
    public List<CompanyReturnFood> throttler() {
        rateLimiter.acquire(1);
        return selectCompanyFood();
    }
}