package com.sree.foodbank.dao;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.util.concurrent.RateLimiter;
import com.sree.foodbank.model.*;
import org.json.simple.JSONArray;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import org.json.simple.parser.*;
import org.json.simple.JSONObject;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigDecimal;

import java.security.GeneralSecurityException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;


@Repository("postgres")
public class CompanyDataAccessService implements CompanyDao {
    private final JdbcTemplate jdbcTemplate;

    String url;
    String user;
    String password;
    String googleKey;
    {
        try {
            File postgresObj = new File("path/to/postgres/secret");
            File googleObj = new File("path/to/google/secret");
            Scanner postgresReader = new Scanner(postgresObj);
            Scanner googleReader = new Scanner(googleObj);
            url = postgresReader.nextLine();
            user = postgresReader.nextLine();
            password = postgresReader.nextLine();
            googleKey = googleReader.nextLine();
            postgresReader.close();
            googleReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    @Autowired
    public CompanyDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CompanyReturn> dashboard(String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException {
        final String sql = "SELECT * FROM company WHERE personid = " + googleToken(token);
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
            return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded);
        });
    }

    @Override
    public boolean login(Login login) throws SQLException {
        final String sql = "SELECT * FROM credentials WHERE username = ? AND password = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, login.getUsername());
            pst.setString(2, login.getPassword());
            ResultSet rs = pst.executeQuery();
            return rs.next();
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
            return new CompanyReturn(0, company.getName(), "", "", new String[]{"asd"}, new String[]{"das"}, jsonObject, "", "");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return new CompanyReturn(0, "asd", "", "", new String[]{"asd"}, new String[]{"das"}, new Object(), "", "");
    }

    @Override
    public int insertFood(BigDecimal id, String token, CompanyFood companyFood) throws GeneralSecurityException, IOException {
        token = companyFood.getToken();
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
            return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded);
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
        final String sql = "SELECT * FROM company";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            String type = companyFilter.getType();
            if (type == null) {
                type = "";
            }
            String newSql = "SELECT * FROM company";
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
                    newSql = "SELECT * FROM company WHERE availablefood @> '" + availableFoodIdArray + "'";
                }
                case "neededFood" -> {
                    neededFoodFilter = companyFilter.getNeededFood();
                    neededFoodIds = foodConvertNametoId(neededFoodFilter);
                    neededFoodIdArray = con.createArrayOf("DECIMAL", neededFoodIds);
                    newSql = "SELECT * FROM company WHERE neededfood @> '" + neededFoodIdArray + "'";
                }
                case "both" -> {
                    neededFoodFilter = companyFilter.getNeededFood();
                    availableFoodFilter = companyFilter.getAvailableFood();
                    neededFoodIds = foodConvertNametoId(neededFoodFilter);
                    availableFoodIds = foodConvertNametoId(availableFoodFilter);
                    neededFoodIdArray = con.createArrayOf("DECIMAL", neededFoodIds);
                    availableFoodIdArray = con.createArrayOf("DECIMAL", availableFoodIds);
                    newSql = "SELECT * FROM company WHERE neededfood @> '" + neededFoodIdArray + "' AND availablefood @> '" + availableFoodIdArray + "'";
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
                return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded);
                //return new CompanyReturn(DBId, name, url, phone, neededFoodNames, availableFoodNames, parsedAddress, userType);
            });

        }
    }


    @Override
    public List<CompanyReturn> selectCompanyById(BigDecimal id) {
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
            String base64Encoded = imageType + Base64.getEncoder().encodeToString(imgBytes);

            return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded);
            //return new CompanyReturn(DBId, name, url, phone, neededFoodNames, availableFoodNames, parsedAddress, userType);
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
        id = googleToken(token);
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
    public int updateCompanybyId(BigDecimal id, String token, Company company) throws GeneralSecurityException, IOException {
        token = company.getToken();
        id = googleToken(token);
        final String sql = "UPDATE company SET name = ?, address = ? WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pstUpdate = con.prepareStatement(sql)) {
            String addressString = String.valueOf(company.getAddress());
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(addressString);
            pstUpdate.setString(1, company.getName());
            pstUpdate.setObject(2, jsonObject);
            pstUpdate.setBigDecimal(3, id);
            pstUpdate.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return 0;
    }

    @Override
    public int updateFood(String token, CompanyFood companyFood) throws GeneralSecurityException, IOException {
        BigDecimal id = googleToken(companyFood.getToken());
        final String sqlUpdate = "UPDATE company SET neededfood = ? WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pstUpdate = con.prepareStatement(sqlUpdate)) {
            String[] foodNames = companyFood.getNeededFood();
            BigDecimal[] foodIds = new BigDecimal[foodNames.length];
            for (int x = 0; x < foodNames.length; x++) {
                BigDecimal temp = foodNameToId(foodNames[x]);
                foodIds[x] = temp;
            }
            BigDecimal[] addFood = getFoodFromId(id);
            BigDecimal[] newFood = new BigDecimal[foodIds.length + addFood.length];

            System.arraycopy(foodIds, 0, newFood, 0, foodIds.length);
            System.arraycopy(addFood, 0, newFood, foodIds.length, addFood.length);

            Array food = con.createArrayOf("DECIMAL", newFood);
            pstUpdate.setArray(1, food);
            pstUpdate.setBigDecimal(2, id);
            pstUpdate.executeUpdate();

        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return 1;
    }

    @Override
    public int requestFood(String token, CompanyRequest companyRequest) throws SQLException, IOException, GeneralSecurityException, MessagingException {
        //final String sql = "INSERT INTO requests (requesterid, receiverid, food, datetime, type, status) VALUES (?, ?, ?, ?, ?, ?)";
        final String sql = "INSERT INTO requests (requesterid, receiverid, food, type, status, show) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            BigDecimal requesterId = googleToken(token);
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
    public void sendMail(int DBId) throws SQLException, MessagingException {
        final String sql = "SELECT * FROM company WHERE personid = ?";
        final String newSql = "SELECT * FROM company WHERE id = ?";
        String recipient = "";
        String sender = "";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(newSql)) {
            pst.setInt(1, DBId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                recipient = rs.getString("email");
            }
        }

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", "smtp.gmail.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.debug", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("","");
                    }
                });
        Transport transport = session.getTransport();
        InternetAddress addressFrom = new InternetAddress("");

        MimeMessage message = new MimeMessage(session);
        message.setSender(addressFrom);
        message.setSubject("This is the subject");
        message.setContent("This is the test message", "text/plain");
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

        transport.connect();
        Transport.send(message);
        transport.close();
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
        }
        return 1;
    }

    @Override
    public List<CompanyReturnRequest> getRequest(String token) throws SQLException, GeneralSecurityException, IOException {
        BigDecimal id = googleToken(token);
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
            pst.setBigDecimal(3, googleToken(imageEncode.getToken()));
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
        GoogleIdToken idToken = verifier.verify(token);
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            String userId = payload.getSubject();
            return new BigDecimal(userId);
        } else {
            return new BigDecimal("null");
        }
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

    final RateLimiter rateLimiter = RateLimiter.create(10.0);
    public List<CompanyReturnFood> throttler() {
        rateLimiter.acquire(1);
        return selectCompanyFood();
    }
}