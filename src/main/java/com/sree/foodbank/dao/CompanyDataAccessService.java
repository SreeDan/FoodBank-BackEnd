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
import org.apache.commons.io.FileUtils;
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
public class CompanyDataAccessService implements CompanyDao { //  Data Access Service layer of the API
    private final JdbcTemplate jdbcTemplate;

    //Stores secret information into these variables
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
            //  Opens and stores the secret information
            String postgresPath = new File("src/main/java/com/sree/foodbank/secret/PostgresSecret.txt").getAbsolutePath();
            File postgresObj = new File(postgresPath);
            String googlePath = new File("src/main/java/com/sree/foodbank/secret/GoogleSecret.txt").getAbsolutePath();
            File googleObj = new File(googlePath);
            String sendGridPath = new File("src/main/java/com/sree/foodbank/secret/SendGridSecret.txt").getAbsolutePath();
            File sendGridObj = new File(sendGridPath);
            String jwtPath = new File("src/main/java/com/sree/foodbank/secret/JWTSecret.txt").getAbsolutePath();
            File jwtObj = new File(jwtPath);
            String emailPath = new File("src/main/java/com/sree/foodbank/secret/Email.txt").getAbsolutePath();
            File emailObj = new File(emailPath);
            Scanner postgresReader = new Scanner(postgresObj);
            Scanner googleReader = new Scanner(googleObj);
            Scanner sendGridReader = new Scanner(sendGridObj);
            Scanner jwtScanner = new Scanner(jwtObj);
            Scanner emailScanner = new Scanner(emailObj);
            // URL to the database
            url = postgresReader.nextLine();
            // Username to the Database
            user = postgresReader.nextLine();
            // Password for the database
            password = postgresReader.nextLine();
            // Google key to decrypt Google tokens
            googleKey = googleReader.nextLine();
            // Google API key to convert addresses into longitude, latitude, and their place_id
            googleGeocodeKey = googleReader.nextLine();
            // Sendgrid key used to send emails
            sendGridKey = sendGridReader.nextLine();
            /// Personalized JWT key used to write and decrypt tokens assigned by the server
            jwtSecret = jwtScanner.nextLine();
            // The email used to send emails.
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

    //  This method decodes a token that I assign so I can identify a user.
    public BigDecimal decodeToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(jwtSecret))
                    .parseClaimsJws(token).getBody();
            //  Returns the subject of the token because it is the id stored in the database
            return new BigDecimal(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<CompanyInfo> dashboard(String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException {
        String sql;
        BigDecimal id;
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the id of the person is something". This gives the data for multiple accounts.
         */
        if (googleToken(token) != null) { //  Tries to convert the token into an id if it is a Google account
            sql = "SELECT * FROM company WHERE personid = " + googleToken(token);
            id = googleToken(token);
        } else { //  Tries to convert the token into an id if it is the server's account
            sql = "SELECT * FROM company WHERE personid = " + decodeToken(token);
            id = decodeToken(token);
        }

        return jdbcTemplate.query(sql, (resultSet, i) -> { //  resultSet is the results of the query search.
            Integer DBId = (Integer) resultSet.getObject("id");
            String name = resultSet.getString("companyname");
            String userType = resultSet.getString("class");
            //  The address is an object in the form of {"Zip": "", "State": "", "Street": "", "City": ""}.
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
            /*  The food is stored as arrays with integers that correspond to food items for example:
                {1, 2} may refer to {'Beans', 'Pasta'}. this changes the array to reflect that.
            */
            if (neededFood != null) {
                try {
                    if (foodConvertIdtoNameJson(neededFood).length() != 0) {
                        neededFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(neededFood));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (availableFood != null) {
                try {
                    if (foodConvertIdtoNameJson(availableFood).length() != 0) {
                        availableFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(availableFood));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            JSONArray neededFoodParsed = (JSONArray) neededFoodJson;
            JSONArray availableFoodParsed = (JSONArray) availableFoodJson;

            String url = resultSet.getString("url");
            String phone = resultSet.getString("phone");

            //  This gets the bytes of the user's image. The image refers to the profile picture.
            byte[] imgBytes = resultSet.getBytes("image");
            //  The imageType is the type of image such as png, jpg, etc.
            String imageType = resultSet.getString("imagetype");
            String base64Encoded = "";
            if (imgBytes != null) { //  Checks to see if a profile picture is present and if one is, it converts it to base64
                base64Encoded = imageType + Base64.getEncoder().encodeToString(imgBytes);
            }
            String email = resultSet.getString("email");
            String[] creds = getUserAndDash(id);
            return new CompanyInfo(DBId, creds[0], creds[1], name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, "", "", "", "", userType, base64Encoded, email, 0.0, 0.0);
        });
    }

    public String[] getUserAndDash(BigDecimal id) throws SQLException {
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the credentials table where the id of the person is ?". This gives the data for one account.
         */
        final String sql = "SELECT * FROM credentials WHERE id = ?";
        try (Connection con = DriverManager.getConnection(url, user, password); //  Establishes a connection to the database
             PreparedStatement pst = con.prepareStatement(sql)) { //  Prepares the SQL statement
            pst.setBigDecimal(1, id); //  Sets the first question mark's value to the ID
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new String[] {rs.getString("username"), rs.getString("password")}; //  Returns the username a password of the user in a String array
            }
            return new String[] {"", ""}; //  If the account is not found, return an array with empty strings.
        }
    }

    @Override
    public String login(Login login) throws SQLException {
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the credentials table where the username is ? and the password is ?". This gives the data for one account.
         */
        final String sql = "SELECT * FROM credentials WHERE username = ? AND password = ?";
        try (Connection con = DriverManager.getConnection(url, user, password); //  Establishes a connection to the database
             PreparedStatement pst = con.prepareStatement(sql)) { //  Prepares the SQL statement
            //  Gets the username and password of the user
            pst.setString(1, login.getUsername());
            pst.setString(2, login.getPassword());
            ResultSet rs = pst.executeQuery();

            Instant now = Instant.now();
            byte[] secret = Base64.getDecoder().decode(jwtSecret); //  Decodes the jwtSecret
            String id = getId(login.getUsername(), login.getPassword()).toString(); //  Gets the id of the user based on their username and password

            String jwt = Jwts.builder() //  Builds the JWT token to assign to the user
                    .setSubject(id) // Sets the subject
                    .setAudience("Food Pantry Pickup") //  Sets the audience
                    .setIssuedAt(Date.from(now)) // Sets the time issued
                    .signWith(Keys.hmacShaKeyFor(secret)) //  Sign the key with the secret
                    .compact();
            return jwt;
        }
    }

    public BigDecimal getId(String user, String password) throws SQLException {
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the credentials table where the username is ? and the password is ?". This gives the data for one account.
         */
        final String sql = "SELECT * FROM credentials WHERE username = ? AND password = ?";
        try (Connection con = DriverManager.getConnection(url, this.user, this.password); //  Establishes a connection to the database
             PreparedStatement pst = con.prepareStatement(sql)) { //  Prepares the SQL statement
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
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the food table in alphabetical order". This gives the data for one account.
         */
        final String sql = "SELECT * from food ORDER BY foodname ASC";
        String[] listFood = {};
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            BigDecimal id = resultSet.getBigDecimal("foodid");
            String food = resultSet.getString("foodname");

            return new Food(id, food); // Returns the id of the food and the food corresponding to the id
        });
    }

    public List<CompanyReturn> returnCompany(String sql, Double distance) {
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
                    if (foodConvertIdtoNameJson(neededFood).length() != 0) {
                        neededFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(neededFood));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (availableFood != null) {
                try {
                    if (foodConvertIdtoNameJson(availableFood).length() != 0) {
                        availableFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(availableFood));
                    }
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
            return new CompanyReturn(DBId, name, url, phone, neededFoodParsed, availableFoodParsed, parsedAddress, userType, base64Encoded, email, distance, 0.0, 0.0);
            //return new CompanyReturn(DBId, name, url, phone, neededFoodNames, availableFoodNames, parsedAddress, userType);
        });
    }

    @Override
    public List<CompanyReturn> selectAllCompanies(CompanyReturn companyReturn) throws SQLException {
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the type of account is a bank". This gives the data for multiple accounts.
         */
        final String sql = "SELECT * FROM company WHERE class = 'bank'";
        return returnCompany(sql, 0.0);
    }

    @Override
    public List<CompanyFood> selectCompanyAvailable(String token) throws GeneralSecurityException, IOException {
        String sql;
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the id of the person is ?". This gives the data for one account.
         */
        if (googleToken(token) != null) {
            sql = "SELECT * FROM company WHERE personid = " + googleToken(token);
        } else {
            sql = "SELECT * FROM company WHERE personid = " + decodeToken(token);
        }
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            Array availableFood = resultSet.getArray("availablefood");
            Object availableFoodJson = null;
            if (availableFood != null) {
                try {
                    if (foodConvertIdtoNameJson(availableFood).length() != 0) {
                        availableFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(availableFood));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            JSONArray availableFoodParsed = (JSONArray) availableFoodJson;
            return new CompanyFood(availableFoodParsed);
        });
    }

    @Override
    public List<CompanyFood> selectCompanyNeeded(String token) throws GeneralSecurityException, IOException {
        String sql;
         /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the id of the person is ?". This gives the data for one account.
         */
        if (googleToken(token) != null) {
            sql = "SELECT * FROM company WHERE personid = " + googleToken(token);
        } else {
            sql = "SELECT * FROM company WHERE personid = " + decodeToken(token);
        }
        return jdbcTemplate.query(sql, (resultSet, i) -> {
            Array neededFood = resultSet.getArray("neededfood");
            Object neededFoodJson = null;
            if (neededFood != null) {
                try {
                    if (foodConvertIdtoNameJson(neededFood).length() != 0) {
                        neededFoodJson = new JSONParser().parse(foodConvertIdtoNameJson(neededFood));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            JSONArray neededFoodParsed = (JSONArray) neededFoodJson;
            return new CompanyFood(neededFoodParsed);
        });
    }

    public String filtering(CompanyFilter companyFilter, String extension) throws SQLException {
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the type of account is a bank". This gives the data for multiple accounts.
         */
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

            switch (type) { //  '@>' in the sql statements means 'contains'
                case "availableFood" -> { //  If the type is availableFood, change the sql statement to search for availableFood.
                    availableFoodFilter = companyFilter.getAvailableFood();
                    availableFoodIds = foodConvertNametoId(availableFoodFilter);
                    availableFoodIdArray = con.createArrayOf("DECIMAL", availableFoodIds);
                    newSql = "SELECT * FROM company WHERE availablefood @> '" + availableFoodIdArray + "' AND class = 'bank'" + extension;
                }
                case "neededFood" -> { //  If the type is neededFood, change the sql statement to search for neededFood.
                    neededFoodFilter = companyFilter.getNeededFood();
                    neededFoodIds = foodConvertNametoId(neededFoodFilter);
                    neededFoodIdArray = con.createArrayOf("DECIMAL", neededFoodIds);
                    newSql = "SELECT * FROM company WHERE neededfood @> '" + neededFoodIdArray + "' AND class = 'bank'" + extension;
                }
                case "both" -> { //  If the type is both, change the sql statement to search for both.
                    neededFoodFilter = companyFilter.getNeededFood();
                    availableFoodFilter = companyFilter.getAvailableFood();
                    neededFoodIds = foodConvertNametoId(neededFoodFilter);
                    availableFoodIds = foodConvertNametoId(availableFoodFilter);
                    neededFoodIdArray = con.createArrayOf("DECIMAL", neededFoodIds);
                    availableFoodIdArray = con.createArrayOf("DECIMAL", availableFoodIds);
                    newSql = "SELECT * FROM company WHERE neededfood @> '" + neededFoodIdArray + "' AND availablefood @> '" + availableFoodIdArray + "' AND class = 'bank'" + extension;
                }
            }
            return newSql;
        }
    }

    @Override
    public List<CompanyReturn> filterByFood(CompanyFilter companyFilter) throws SQLException {
        String newSql = filtering(companyFilter, "");
        return returnCompany(newSql, 0.0);
    }


    @Override
    public List<CompanyReturn> selectCompanyById(BigDecimal id, Double distance) {
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the id is the id in the parameter". This gives the data for a single account.
         */
        final String sql = "SELECT * FROM company WHERE id = " + id;
        return returnCompany(sql, 0.0);
    }

    @Override
    public List<CompanyNeededFood> getCompanyNeededFoodById(BigDecimal id) {
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the id is the id in the parameter". This gives the data for a single account.
         */
        final String sql = "SELECT * FROM company WHERE id = " + id;
        return jdbcTemplate.query(sql, (resultSet, i) -> { //  resultSet is the results of the query search
            //  The three lines get the user's id, name, and type (user or food bank) from the database
            //  Gets the neededFood and parses it
            Array neededFood = resultSet.getArray("neededFood");
            String[] neededFoodNames = {};
            if (neededFood != null) {
                neededFoodNames = foodConvertIdtoName(neededFood);
            }
            return new CompanyNeededFood(neededFoodNames);
        });
    }

    @Override
    public int deleteCompanybyId(String token) throws GeneralSecurityException, IOException, SQLException {
        BigDecimal id;
        if (googleToken(token) != null) {
            id = googleToken(token);
        }
        else {
            id = decodeToken(token);
        }
        String sql = "DELETE FROM company WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            pst.executeUpdate();
        }
        sql = "DELETE FROM credentials WHERE id = ?";
        try (Connection con = DriverManager.getConnection(url, user,password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            pst.executeUpdate();
        }
        return 0;
    }

    @Override
    public int updateCompanybyId(String token, CompanyInfo companyUpdate) throws GeneralSecurityException, IOException, InterruptedException {
        BigDecimal id;
        if (googleToken(token) != null) { //  Tries to convert the token into an id if it is a Google account
            id = googleToken(token);
        }
        else { //  Tries to convert the token into an id if it is the server's account
            id = decodeToken(token);
        }

        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "Update the company table where the id is the ?, and set the columns address, url, email... to ?, ?, ?...". This updates the records for a single account.
         */
        final String sql = "UPDATE company SET address = ?::JSON, url = ?, email = ?, phone = ?, image = ?, imagetype = ?, lat = ?, long = ?, place_id = ? WHERE personid = ?";
        String user = companyUpdate.getUser();
        String pass = companyUpdate.getPassword();
        String billing = companyUpdate.getBilling();
        String city = companyUpdate.getCity();
        String state = companyUpdate.getState();
        String ZIP = companyUpdate.getZIP();

        //  Calls the latlng method to get the latitude, longitude, and place_id of the address
        String[] latLng = getLatLng(billing, city, state, ZIP);
        if (latLng[0] == null) { //  Checks if there is an error
            if (latLng[3] == null) { //  Checks if the Google server has an error
                throw new ApiRequestException("Error Validating Your Address - Please Try Again Later");
            }
            throw new ApiRequestException("Invalid Address");
        }

        String url = companyUpdate.getUrl();
        String phone = companyUpdate.getPhone();
        String email = companyUpdate.getEmail();
        String image = companyUpdate.getImage();

        //  Creates the address
        String[] address;
        if (billing == null || city == null || state == null || state == null) {
            address = new String[] {"{\"ZIP\": \"null\", \"City\": \"null\", \"State\": \"null\", \"Street\": \"null\"}"};
        } else {
            address = new String[] {"{\"ZIP\": \"" + ZIP + "\", \"City\": \"" + city + "\", \"State\": \"" + state + "\", \"Street\": \"" + billing + "\"}"};
        }

        if (image.isEmpty()) { //  If there is no profile picture, call the updateWithoutImage method and update the record without the image.
            updateWithoutImage(user, pass, address, url, email, phone, latLng[0], latLng[1], latLng[2], id);
            return 1;
        }

        //  Parses the image and converts it into a string and bytea for the database
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
        String path = new File("src/main/java/com/sree/foodbank/images/test_image." + extension).getAbsolutePath();
        File file = new File(path);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File newFile = new File(path);
        FileInputStream fis = new FileInputStream(file);

        updateUserPass(user, pass, id); //  Update the username and password
        try (Connection con = DriverManager.getConnection(this.url, this.user, password); //  Establishes a connection to the databas
             PreparedStatement pst = con.prepareStatement(sql)) { //  Prepares the SQL statement
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
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "Update the company table where the id is the ?, and set the columns address, url, email... to ?, ?, ?...". This updates the records for a single account.
         */
        String sql = "UPDATE company SET address = ?::JSON, url = ?, email = ?, phone = ?, lat = ?, long = ?, place_id = ? WHERE personid = ?";
        updateUserPass(user, pass, id);
        try (Connection con = DriverManager.getConnection(this.url, this.user, this.password); //  Establishes a connection to the databas
             PreparedStatement pst = con.prepareStatement(sql)) { //  Prepares the SQL statement
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
    public int updateFood(String token, CompanyUpdateFood foodUpdate) throws GeneralSecurityException, IOException, SQLException {
        BigDecimal id;
        if (googleToken(token) != null) {
            id = googleToken(token);
        }
        else {
            id = decodeToken(token);
        }

        List<Food> foods = foodUpdate.getFood();

        BigDecimal[] updatedFood = new BigDecimal[foods.size()];
        for (int x = 0; x < foods.size(); x++) {
            updatedFood[x] = foods.get(x).getId();
        }
        final String sql = "UPDATE company SET " + foodUpdate.getType() + " = ? WHERE personid = ?";
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
        /*  This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "insert the columns requesterid, receiverid,... into the requests table with the values of ?, ?,...". This inserts one record.
         */
        final String sql = "INSERT INTO requests (requesterid, receiverid, food, type, status, show) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            BigDecimal requesterId;
            if (googleToken(token) != null) {  //  Tries to convert the token into an id if it is a Google account
                requesterId = googleToken(token);
            }
            else { //  Tries to convert the token into an id if it is the server's account
                requesterId = decodeToken(token);
            }
            BigDecimal receiverId = getId(companyRequest.getReceiverId());
            BigDecimal[] foodIds = foodConvertNametoId(companyRequest.getFood());
            Array foodArray = con.createArrayOf("DECIMAL", foodIds);

            pst.setBigDecimal(1, requesterId);
            pst.setBigDecimal(2, receiverId);
            pst.setArray(3, foodArray);
            pst.setString(4, companyRequest.getType());
            pst.setString(5, companyRequest.getStatus());
            pst.setBoolean(6, true);
            pst.executeUpdate();
        }
        return 1;
    }

    @Override
    public int updateRequest(CompanyRequest companyRequest) throws SQLException, GeneralSecurityException, IOException {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "Update the request table where the request is the ?, and set the columns address and datetime to ?, ?". This updates the records for a single request.
         */
        final String sql = "UPDATE requests SET status = ?, datetime = ? WHERE requestId = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            String dateString = companyRequest.getDate();
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
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the request table where the requestid is the id in the parameter". This gives the data for a single request.
         */
        final String sql = "SELECT * FROM requests WHERE requestid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, requestId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                BigDecimal requesterId = rs.getBigDecimal("requesterId");
                BigDecimal receiverId = rs.getBigDecimal("receiverId");
                String[] requesterInfo = info(requesterId); //  Info about the requester
                String[] receiverInfo = info(receiverId); //  Info about the receiver
                return new String[]{requesterInfo[0], requesterInfo[1], receiverInfo[0], receiverInfo[2]};
            }
            return new String[0];
        }
    }

    public String[] info(BigDecimal id) throws SQLException {
         /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the id is the id in the parameter". This gives the data for a single account.
         */
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
        if (googleToken(token) != null) { //  Tries to convert the token into an id if it is a Google account
            id = googleToken(token);
        }
        else { //  Tries to convert the token into an id if it is the server's account
            id = decodeToken(token);
        }
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select the columns from the request table where the requesterid is the id or the receiverid is the id". This gives the data for a many requests.
         */
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

    public String getName(BigDecimal id) throws SQLException {
         /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select the name from the company table where the user's id is the id in the parameter". This gives the data for one user.
         */
        final String sql = "SELECT companyname FROM company WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            ResultSet rs = pst.executeQuery();
            String name = "";
            while (rs.next()) { //  While there is another row
                name = rs.getString("companyname");
            }
            return name;
        }
    }

    public BigDecimal getId(int id) throws SQLException {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select the id from the company table where the user's id is the id in the parameter". This gives the data for one user.
         */
        final String sql = "SELECT personid FROM company WHERE id = ?";
        try(Connection con = DriverManager.getConnection(url, user, password);
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            BigDecimal personId = BigDecimal.valueOf(0);
            while (rs.next()) { //  While there is another row
                personId = rs.getBigDecimal("personid");
            }
            return personId;
        }
    }

    @Override
    public BigDecimal googleToken(String token) throws GeneralSecurityException, IOException { // Converts the token to an id using Google's library
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

    @Override
    public void sendEmail(String[] info, String date) throws IOException, ParseException { //  Sends an email using Sendgrid
        Email from = new Email(email);
        String subject = "Request has been approved, check timings";
        Email to = new Email(info[1]);
        JSONParser parser = new JSONParser();
        JsonObject obj = new JsonParser().parse(info[3]).getAsJsonObject();
        String address = String.join(", ", obj.get("Street").getAsString(), obj.get("City").getAsString(), obj.get("State").getAsString() + " " + obj.get("ZIP").getAsString());
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
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "insert into the credentials table and set the columns username, password, id to ?, ?, ?". This gives the data for one food.
         */
        final String sql = "INSERT INTO credentials (username, password, id) VALUES (?, ?, ?)";
        double min = 100000000000000000000.0;
        double max = 999999999999999999999.0;
        Random random = new Random();
        double randomValue = min + (max - min) * random.nextDouble(); //  Creates an id for the new user
        BigDecimal id = new BigDecimal(randomValue);

        checkUsernameAvailability(createAccount); //  Checks if the username is available

        boolean status = addInfo(id, createAccount.getName(), createAccount);
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

    public void checkUsernameAvailability(CreateAccount createAccount) {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the credentials table where the username is ?". This gives the data for one food.
         */
        final String sql = "SELECT * FROM credentials WHERE username = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, createAccount.getUsername());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) { //  If there is a username
                throw new ApiRequestException("Username Taken");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public List<CompanyReturn> locationFilter(ArrayList<Integer> ids, ArrayList<String> place_ids, Location location) throws SQLException, IOException, InterruptedException {
        int placeholder = 0;

        //  Creating 2d array of 25 locations each beacuse you can only send 25 locations at once to the Google API
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
        //  Calling a request the Google geocoding API with 25 places at a time
        for (int rows = 0; rows < distance.length; rows++) {
            String origin = location.getLat().toString() + "," + location.getLng().toString();
            StringBuilder destinations = new StringBuilder();
            for (int cols = 0; cols < distance[rows].length; cols++) {
                String place_id = place_ids_array[rows][cols];
                if (place_id != null) {
                    destinations.append("place_id:").append(place_id).append("|"); //  Seperate each place_id with "|"
                }
            }

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial")
                    .queryParam("origins", origin)
                    .queryParam("destinations", destinations)
                    .queryParam("key", googleGeocodeKey)
                    .build()
                    .encode()
                    .toUriString(); // Building the URL to send the request

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

            //  Put the distances in the 2d distance array
            for (int cols = 0; cols < distance[rows].length; cols++) {
                if (place_ids_array[rows][cols] != null) {
                    String distanceText = res.getJSONObject(cols).getJSONObject("distance").getString("text");
                    distanceText = distanceText.replace(",", "");
                    distance[rows][cols] = Double.valueOf(distanceText.substring(0, distanceText.indexOf(" ")));
                    num_distances++;
                }
            }
        }

        //  Put the distance and id of the place into an array of Distance objects. Each object holds the id and distance so they are together when sorting.
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
        Distance[] sorted = mergeSort(single_distances); //  Sorts the foodbanks by distance
        ArrayList<CompanyReturn> companyReturns = new ArrayList<CompanyReturn>();
        List<CompanyReturn> companyReturnList = new ArrayList<CompanyReturn>();
        for (Distance d: sorted) {
            companyReturnList.add(selectCompanyById(BigDecimal.valueOf(d.getId()), d.getDistance()).get(0)); //  Adds each company to a list to return as a response to the request
        }

        return companyReturnList;
    }

    @Override
    public List<CompanyReturn> locationFiltering(Location location) throws SQLException, IOException, InterruptedException {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the credentials table where the user is a food bank and the state they are in is ?". This gives the data for many food banks.
         */
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
        if (ids.size() == 0) {
            throw new ApiRequestException("None");
        }
        return locationFilter(ids, place_ids, location);
    }

    public Distance[] mergeSort(Distance[] array) { //  Standard merge sort method
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

    public Distance[] merge(Distance[] left, Distance[] right) { //  Standard merge method
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

    @Override
    public List<CompanyReturn> bothFilter(CompanyBothFilter companyBothFilter) throws SQLException, IOException, InterruptedException {
        if (companyBothFilter.getStandard()) {
            CompanyFilter companyFilter = new CompanyFilter(companyBothFilter.getType(), companyBothFilter.getAvailableFood(), companyBothFilter.getNeededFood());
            Location location = new Location(companyBothFilter.getLat(), companyBothFilter.getLng(), "PA", true);
            String newSql = "SElECT * FROM company WHERE class = 'bank' AND address ->> 'State' = 'PA'";
            List<CompanyReturn> companies = returnCompany(newSql, 0.0);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            ArrayList<String> place_ids = new ArrayList<String>();
            for (CompanyReturn company: companies) {
                ids.add(company.getId());
                place_ids.add(getPlace_Ids(company.getId()));
            }
            if (ids.size() == 0) {
                throw new ApiRequestException("None");
            }
            return locationFilter(ids, place_ids, location);
        }
        CompanyFilter companyFilter = new CompanyFilter(companyBothFilter.getType(), companyBothFilter.getAvailableFood(), companyBothFilter.getNeededFood());
        Location location = new Location(companyBothFilter.getLat(), companyBothFilter.getLng(), companyBothFilter.getState(), false);
        String newSql = filtering(companyFilter, " AND address ->> 'State' = '" + location.getState() + "'");
        List<CompanyReturn> companies = returnCompany(newSql, 0.0);
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ArrayList<String> place_ids = new ArrayList<String>();
        for (CompanyReturn company: companies) {
            ids.add(company.getId());
            place_ids.add(getPlace_Ids(company.getId()));
        }
        if (ids.size() == 0) {
            throw new ApiRequestException("None");
        }
        return locationFilter(ids, place_ids, location);
    }

    public String getPlace_Ids(Integer id) throws SQLException {
        final String sql = "SELECT * FROM company WHERE id = " + id;
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                return rs.getString("place_id");
            }
        }
        throw new ApiRequestException("Something went wrong");
    }

    public boolean addInfo(BigDecimal id, String name, CreateAccount createAccount) throws SQLException {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the credentials table where the user is a food bank and the state they are in is ?". This gives the data for many food banks.
         */
        final String sql = "INSERT INTO company (personid, companyname, email, class, address, availableFood, neededFood, lat, long, place_id, image, imagetype) VALUES (?, ?, ?, ?, ?::JSON, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {

            String billing = createAccount.getBilling();
            String city = createAccount.getCity();
            String state = createAccount.getState();
            String ZIP = createAccount.getZIP();

            String[] latLng = getLatLng(billing, city, state, ZIP); //  Calls the latlng method to get the latitude, longitude, and place_id of the address
            if (latLng[0] == null) { //  Checks if there is an error
                if (latLng[3] == null) { //  Checks if the Google server has an error
                    throw new ApiRequestException("Error Creating an Account at this Time");
                }
                throw new ApiRequestException("Invalid Address");
            }

            //  Creates the address
            String[] address;
            if (createAccount.getBilling() == null || createAccount.getCity() == null || createAccount.getState() == null || createAccount.getZIP() == null) {
                address = new String[] {"{\"ZIP\": \"null\", \"City\": \"null\", \"State\": \"null\", \"Street\": \"null\"}"};
            } else {
                address = new String[] {"{\"ZIP\": \"" + ZIP + "\", \"City\": \"" + city + "\", \"State\": \"" + state + "\", \"Street\": \"" + billing + "\"}"};
            }

            Array availableFoodArray = con.createArrayOf("DECIMAL", new BigDecimal[] {});
            Array neededFoodArray = con.createArrayOf("DECIMAL", new BigDecimal[] {});

            //  Adds a standard profile picture as default
            String path = new File("src/main/java/com/sree/foodbank/images/nopfp-v2.png").getAbsolutePath();
            byte[] fileContent = FileUtils.readFileToByteArray(new File(path));
            String encodedString = Base64.getEncoder().encodeToString(fileContent);

            //  Parses the image and converts it into a string and bytea for the database
            String[] strings = {"data:image/png;base64,", encodedString};
            String extension = "png";
            String fullExtension = "data:image/png;base64,";

            byte[] data = DatatypeConverter.parseBase64Binary(strings[1]);
            File file = new File(path);
            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            File newFile = new File(path);
            FileInputStream fis = new FileInputStream(file);

            pst.setBigDecimal(1, id);
            pst.setString(2, name);
            pst.setString(3, createAccount.getEmail());
            pst.setString(4, createAccount.getType());
            pst.setObject(5, address[0]);
            pst.setArray(6, availableFoodArray);
            pst.setArray(7, neededFoodArray);
            pst.setString(8, latLng[0]);
            pst.setString(9, latLng[1]);
            pst.setString(10, latLng[2]);
            pst.setBinaryStream(11, fis, newFile.length());
            pst.setString(12, fullExtension);
            pst.executeUpdate();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String[] getLatLng(String billing, String city, String state, String ZIP) throws IOException, InterruptedException { //  Gets the latitude, longitude, and place_id of an address
        billing = billing.replace(' ', '+');
        city = city.replace(' ', '+');
        final String POSTS_API_URL = "https://maps.googleapis.com/maps/api/geocode/json?address=" + billing + ",+" + city + ",+" + state + "&key=" + googleGeocodeKey;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("accept", "application/json")
                .uri(URI.create(POSTS_API_URL))
                .build(); //  Builds the request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String s = response.body();
        org.json.JSONObject obj = new org.json.JSONObject(s);

        String[] values = new String[3];
        try {
            if (obj.getString("status").equals("ZERO_RESULTS")) { //  Check if it is an invalid adress
                values[0] = null;
                values[1] = null;
                values[2] = null;
                values[3] = "0.0";
                return values;
            }
        } catch (Exception e) {}
        org.json.JSONObject res = obj.getJSONArray("results").getJSONObject(0);
        org.json.JSONObject loc = res.getJSONObject("geometry").getJSONObject("location");
        try {
            if (res.getBoolean("partial_match")) { //  Check if it is an invalid adress
                values[0] = null;
                values[1] = null;
                values[2] = null;
                values[3] = "0.0";
                return values;
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
        values[3] = null; //  This null value means that the request did not get a response.
        return values;
    }

    @Override
    public void checkGoogleAccount(Token token) throws SQLException, GeneralSecurityException, IOException {
        BigDecimal id = googleToken(token.getToken());
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select all columns from the company table where the user id is ?". This gives the data for one account.
         */
        final String sql = "SELECT * FROM company WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBigDecimal(1, id);
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) {
                throw new ApiRequestException("No Account");
            }
        }
    }

    @Override
    public boolean createGAccount(CreateAccount createAccount) throws SQLException {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "insert the columns personid, companyname,.. into the company table with the values of ?, ?,...". This inserts one record.
         */
        final String sql = "INSERT INTO company (personid, companyname, availablefood, neededfood, address, class, email, image, imagetype) VALUES (?, ?, ?, ?, ?::JSON, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            BigDecimal id = googleToken(createAccount.getToken());
            String billing = createAccount.getBilling();
            String city = createAccount.getCity();
            String state = createAccount.getState();
            String ZIP = createAccount.getZIP();

            String[] latLng = getLatLng(billing, city, state, ZIP); //  Calls the latlng method to get the latitude, longitude, and place_id of the address
            if (latLng[0] == null) { //  Checks if there is an error
                if (latLng[3] == null) { //  Checks if the Google server has an error
                    throw new ApiRequestException("Error Creating an Account at this Time");
                }
                throw new ApiRequestException("Invalid Address");
            }

            //  Creates the address
            String[] address;
            if (createAccount.getBilling() == null || createAccount.getCity() == null || createAccount.getState() == null || createAccount.getZIP() == null) {
                address = new String[] {"{\"ZIP\": \"null\", \"City\": \"null\", \"State\": \"null\", \"Street\": \"null\"}"};
            } else {
                address = new String[] {"{\"ZIP\": \"" + ZIP + "\", \"City\": \"" + city + "\", \"State\": \"" + state + "\", \"Street\": \"" + billing + "\"}"};
            }
            Array availableFoodArray = con.createArrayOf("DECIMAL", new BigDecimal[] {});
            Array neededFoodArray = con.createArrayOf("DECIMAL", new BigDecimal[] {});

            //  Adds a standard profile picture as default
            String path = new File("src/main/java/com/sree/foodbank/images/nopfp-v2.png").getAbsolutePath();
            byte[] fileContent = FileUtils.readFileToByteArray(new File(path));
            String encodedString = Base64.getEncoder().encodeToString(fileContent);

            //  Parses the image and converts it into a string and bytea for the database
            String[] strings = {"data:image/png;base64,", encodedString};
            String extension = "png";
            String fullExtension = "data:image/png;base64,";
            byte[] data = DatatypeConverter.parseBase64Binary(strings[1]);
            File file = new File(path);
            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            File newFile = new File(path);
            FileInputStream fis = new FileInputStream(file);

            pst.setBigDecimal(1, id);
            pst.setString(2, createAccount.getName());
            pst.setObject(3, availableFoodArray);
            pst.setObject(4, neededFoodArray);
            pst.setObject(5, address[0]);
            pst.setString(6, createAccount.getType());
            pst.setString(7, createAccount.getEmail());
            pst.setBinaryStream(8, fis, newFile.length());
            pst.setString(9, fullExtension);
            pst.executeUpdate();

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String foodIdToName(BigDecimal foodId) throws SQLException {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select the food name from the food table where the food's id is the id in the parameter". This gives the data for one food.
         */
        final String sql = "SELECT foodname FROM food WHERE foodid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pstGet = con.prepareStatement(sql)) {
            pstGet.setBigDecimal(1, foodId);
            ResultSet rs = pstGet.executeQuery();
            if (rs.next()) {
                String food = rs.getString("foodname");
                return food;
            }
            return null;
        }

    }

    @Override
    public BigDecimal foodNameToId(String foodName) throws SQLException {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select the food id from the food table where the food's name is the name in the parameter". This gives the data for one food.
         */
        final String sql = "SELECT foodid FROM food WHERE foodname = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, foodName);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) { //  If there is another row
                BigDecimal id = rs.getBigDecimal("foodid");
                return id;
            }
            return BigDecimal.valueOf(0);
        }
    }

    public BigDecimal[] getFoodFromId(BigDecimal id) throws SQLException {
         /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "select the neededfood column from the company table where the user id is ?". This gives the data for one account.
         */
        final String sqlGet = "SELECT neededfood FROM company WHERE personid = ?";
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pstGet = con.prepareStatement(sqlGet)) {
            pstGet.setBigDecimal(1, id);
            ResultSet rs = pstGet.executeQuery();
            if (rs.next()) { //  If there is another row
                Array food = rs.getArray("neededfood");
                return (BigDecimal[]) food.getArray();
            }
            return new BigDecimal[0];
        }

    }

    public String[] foodConvertIdtoName(Array food) throws SQLException { //  Converts an array of food to an array of food names
        BigDecimal[] foodString = (BigDecimal[]) food.getArray();
        String[] foodNames = new String[foodString.length];
        for (int x = 0; x < foodString.length; x++) {
            String temp = foodIdToName(foodString[x]);
            foodNames[x] = temp;
        }
        return foodNames;
    }

    public BigDecimal[] foodConvertNametoId(String[] foodNames) throws SQLException { //  Converts an array of food names to an array of food ids
        BigDecimal[] foodIds = new BigDecimal[foodNames.length];
        for (int x = 0; x < foodNames.length; x++) {
            BigDecimal temp = foodNameToId(foodNames[x]);
            foodIds[x] = temp;
        }
        return foodIds;
    }

    public String[] foodConvertIdToNameBeforeJson(BigDecimal[] foodId) throws SQLException { //  Converts food ids to food names without JSON format
        String[] foodString = new String[foodId.length];
        for (int x = 0; x < foodId.length; x++) {
            foodString[x] = foodIdToName(foodId[x]);
        }
        Arrays.sort(foodString);
        return foodString;
    }

    public String foodConvertIdtoNameJson(Array food) throws SQLException { //  Converts food ids to food names with JSON format
        BigDecimal[] foodId = (BigDecimal[]) food.getArray();
        StringBuilder tempString = new StringBuilder();
        String[] foodName = foodConvertIdToNameBeforeJson(foodId);
        for (int x = 0; x < foodId.length; x++) {
            tempString.append(", {\"value\": \"").append(foodNameToId(foodName[x])).append("\", \"label\": \"").append(foodName[x]).append("\"}");
        }
        String newFoodString = "";
        if (tempString.length() != 0)
            newFoodString += "[" + tempString.substring(2) + "]"; //  Creates thew format for the JSON response. It is inside a list.
        return newFoodString;
    }

    public void updateUserPass(String user, String password, BigDecimal id)  {
        /*
            This creates the sql statement that the server will use in the query search.
            The SQL statement is saying to "update the credentials table where the id is the ?, and set the columns username and password to ? and ?".
            This updates tge records for one account.
         */
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
}