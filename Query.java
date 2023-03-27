
//An Hong Nguyen  CSE 344 F2021


package flightapp;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Runs queries against a back-end database
 */
public class Query {
  // DB Connection
  private Connection conn;
  private String currentUserName;

  // Password hashing parameter constants
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;

  private List<FlightType> flightList;

  // Canned queries
  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  // For check dangling
  private static final String TRANCOUNT_SQL = "SELECT @@TRANCOUNT AS tran_count";
  private PreparedStatement tranCountStatement;

  // Verify username and password login
  private static final String LOGIN_SQL = "SELECT * FROM Users WHERE username = ? AND password = ?";
  private PreparedStatement loginStatement;

  // Check to see if the username exists
  private static final String USERNAMEEXIST_SQL = "SELECT * FROM Users WHERE username = ?";
  private PreparedStatement checkUserNameStatement;

  // Create a new user
  private static final String CREATEUSER_SQL = "INSERT INTO Users VALUES(?,?,?,?)";
  private PreparedStatement createUserStatement;

  // Return the query for top n direct flights
  private static final String DIRECTFLIGHT_SQL = "SELECT TOP (?) fid, day_of_month, carrier_id, flight_num, "
                                      + "origin_city, dest_city, actual_time, capacity, price FROM Flights WHERE origin_city = ? "
                                      + "AND dest_city = ? AND day_of_month = ? AND canceled = 0 "
                                      + "ORDER BY actual_time, fid ASC";
  private PreparedStatement directFlightStatement;

  private static final String ONEHOPFLIGHT_SQL = "SELECT TOP (?) f1.fid AS f1_fid, f2.fid AS f2_fid, f1.day_of_month AS f1_day, "
          + "f2.day_of_month AS f2_day, f1.carrier_id AS f1_cid, f2.carrier_id AS f2_cid, "
          + "f1.flight_num AS f1_num, f2.flight_num AS f2_num, f1.origin_city AS f1_origin, "
          + "f2.origin_city AS f2_origin, f1.dest_city AS f1_dest, f2.dest_city AS f2_dest, "
          + "f1.actual_time AS f1_time, f2.actual_time AS f2_time, f1.capacity AS f1_capacity, "
          + "f2.capacity AS f2_capacity, f1.price AS f1_price, f2.price AS f2_price "
          + "FROM Flights AS f1, Flights AS f2 WHERE f1.origin_city = ? AND f2.dest_city = ? AND "
          + "f1.dest_city = f2.origin_city AND f1.day_of_month = ? AND f1.day_of_month = f2.day_of_month "
          + "AND f1.canceled = 0 AND f2.canceled = 0 ORDER BY (f1.actual_time + f2.actual_time), f1_fid , f2_fid ASC";
  private PreparedStatement onehopFlightStatement;

  public Query() throws SQLException, IOException {
    this(null, null, null, null);
  }

  protected Query(String serverURL, String dbName, String adminName, String password)
      throws SQLException, IOException {
    conn = serverURL == null ? openConnectionFromDbConn()
        : openConnectionFromCredential(serverURL, dbName, adminName, password);

    prepareStatements();
  }

  /**
   * Return a connecion by using dbconn.properties file
   *
   * @throws SQLException
   * @throws IOException
   */
  public static Connection openConnectionFromDbConn() throws SQLException, IOException {
    // Connect to the database with the provided connection configuration
    Properties configProps = new Properties();
    configProps.load(new FileInputStream("dbconn.properties"));
    String serverURL = configProps.getProperty("flightapp.server_url");
    String dbName = configProps.getProperty("flightapp.database_name");
    String adminName = configProps.getProperty("flightapp.username");
    String password = configProps.getProperty("flightapp.password");
    return openConnectionFromCredential(serverURL, dbName, adminName, password);
  }

  /**
   * Return a connecion by using the provided parameter.
   *
   * @param serverURL example: example.database.widows.net
   * @param dbName    database name
   * @param adminName username to login server
   * @param password  password to login server
   *
   * @throws SQLException
   */
  protected static Connection openConnectionFromCredential(String serverURL, String dbName,
      String adminName, String password) throws SQLException {
    String connectionUrl =
        String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
            dbName, adminName, password);
    Connection conn = DriverManager.getConnection(connectionUrl);

    // By default, automatically commit after each statement
    conn.setAutoCommit(true);

    // By default, set the transaction isolation level to serializable
    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    return conn;
  }

  /**
   * Get underlying connection
   */
  public Connection getConnection() {
    return conn;
  }

  /**
   * Closes the application-to-database connection
   */
  public void closeConnection() throws SQLException {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {

      conn.setAutoCommit(false);
      Statement clear = conn.createStatement();
      clear.executeUpdate("DELETE FROM Reservations");
      clear.executeUpdate("DELETE FROM Users");
      conn.commit();
      conn.setAutoCommit(true);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }



  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
    tranCountStatement = conn.prepareStatement(TRANCOUNT_SQL);
    loginStatement = conn.prepareStatement(LOGIN_SQL);
    createUserStatement = conn.prepareStatement(CREATEUSER_SQL);
    checkUserNameStatement = conn.prepareStatement(USERNAMEEXIST_SQL);
    directFlightStatement = conn.prepareStatement(DIRECTFLIGHT_SQL);
    onehopFlightStatement = conn.prepareStatement(ONEHOPFLIGHT_SQL);
    // TODO: YOUR CODE HERE
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n" For all other
   *         errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password)  {
    try {
      // TODO: YOUR CODE HERE

      if (this.currentUserName != null) {
        return "User already logged in\n";
      }

      String loweredCase_username = username.toLowerCase(Locale.ROOT);
      conn.setAutoCommit(false);
      checkUserNameStatement.clearParameters();
      checkUserNameStatement.setString(1, loweredCase_username);
      ResultSet checkUserResult = checkUserNameStatement.executeQuery();


      byte[] salt = new byte[16];
      while(checkUserResult.next()){
        salt = checkUserResult.getBytes("salt");
      }
      checkUserResult.close();

      // Specify the hash parameters
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
              HASH_STRENGTH, KEY_LENGTH);
      // Generate the hash
      SecretKeyFactory factory = null;
      byte[] hash = null;
      try {
        factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        hash = factory.generateSecret(spec).getEncoded();
      } catch (NoSuchAlgorithmException | InvalidKeySpecException ex)
      {
        throw new IllegalStateException();
      }


      loginStatement.clearParameters();
      loginStatement.setString(1, loweredCase_username);
      loginStatement.setBytes(2, hash);
      ResultSet rs = loginStatement.executeQuery();

      if (rs.next()) {
        this.currentUserName = loweredCase_username;
        conn.commit();
        conn.setAutoCommit(true);
        rs.close();
        return "Logged in as " + username + "\n";
      } else {
        conn.rollback();
        conn.setAutoCommit(true);
        rs.close();
        return "Login failed\n";
      }

      } catch (SQLException e) {
      try {
        conn.rollback();
        ;
        conn.setAutoCommit(true);
      } catch (SQLException throwables) {
        throwables.printStackTrace();
      }
      if (isDeadLock(e)){
        return transaction_login(username,password);
      }
      e.printStackTrace();
      return "Login failed\n";
    }
    finally {
      checkDanglingTransaction();
    }
}

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    try {

      if(initAmount < 0) {
        return "Failed to create user\n";
      }

      String lowerCased_username = username.toLowerCase(Locale.ROOT);

      conn.setAutoCommit(false);
      checkUserNameStatement.clearParameters();
      checkUserNameStatement.setString(1, lowerCased_username);
      ResultSet checkUserNameResult = checkUserNameStatement.executeQuery();

      if(checkUserNameResult.next()){   //true if username already existed
        conn.rollback();   //undo all changes
        conn.setAutoCommit(true);
        checkUserNameResult.close();
        return "Failed to create user\n";
      }

      if (username.length() > 20 || password.length() >20) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Failed to create user\n";
      }


      createUserStatement.clearParameters();
      createUserStatement.setString(1, lowerCased_username);

      // Generate a random cryptographic salt
      SecureRandom random = new SecureRandom();
      byte[] salt = new byte[16];
      random.nextBytes(salt);
      // Specify the hash parameters
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
              HASH_STRENGTH, KEY_LENGTH);
      // Generate the hash
      SecretKeyFactory factory = null;
      byte[] hash = null;
      try {
        factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        hash = factory.generateSecret(spec).getEncoded();
      } catch (NoSuchAlgorithmException | InvalidKeySpecException ex)
      {
        throw new IllegalStateException();
      }


      createUserStatement.setBytes(2, hash);  //Insert hashed password
      createUserStatement.setBytes(3, salt);  //Insert salt value
      createUserStatement.setInt(4, initAmount);
      createUserStatement.execute();
      conn.commit();
      conn.setAutoCommit(true);
      return "Created user " + username + "\n";
    } catch (SQLException throwables) {
      try {
        conn.rollback();;
        conn.setAutoCommit(true);
      } catch (SQLException e) {
        e.printStackTrace();
      }
      if (isDeadLock(throwables)) {
        return transaction_createCustomer(username, password, initAmount);
      }

      throwables.printStackTrace();
      return "Failed to create user\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given day
   * of the month. If {@code directFlight} is true, it only searches for direct flights, otherwise
   * is searches for direct flights and flights with two "hops." Only searches for up to the number
   * of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */

//  private static final String DIRECTFLIGHT_SQL = "SELECT TOP (?) fid, day_of_month, carrier_id, flight_num,"
//          + "origin_city, dest_city, actual_time, capacity, price FROM Flights WHERE origin_city = ?"
//          + "AND dest_city = ? AND day_of_month = ? AND canceled = 0 0RDER BY actual_time, fid ASC";


  public String transaction_search(String originCity, String destinationCity, boolean directFlight,
      int dayOfMonth, int numberOfItineraries) {
    try {
      // WARNING the below code is unsafe and only handles searches for direct flights
      // You can use the below code as a starting reference point or you can get rid
      // of it all and replace it with your own implementation.
      //
      // TODO: YOUR CODE HERE

      int k = 0;

      StringBuffer sb = new StringBuffer();

      //Find direct flights
      directFlightStatement.clearParameters();
      directFlightStatement.setInt(1, numberOfItineraries);
      directFlightStatement.setString(2, originCity);
      directFlightStatement.setString(3, destinationCity);
      directFlightStatement.setInt(4, dayOfMonth);
      ResultSet resultSet = directFlightStatement.executeQuery();

      flightList = new ArrayList<>();
      while (resultSet.next()) {

        Flight f1 = new Flight();
        f1.fid = resultSet.getInt("fid");
        f1.dayOfMonth = resultSet.getInt("day_of_month");
        f1.carrierId = resultSet.getString("carrier_id");
        f1.flightNum = resultSet.getString("flight_num");
        f1.originCity = resultSet.getString("origin_city");
        f1.destCity = resultSet.getString("dest_city");
        f1.time = resultSet.getInt("actual_time");
        f1.capacity = resultSet.getInt("capacity");
        f1.price = resultSet.getInt("price");


        Flight f2 = new Flight();
        f2.time = 0;

        FlightType direct = new FlightType();
        direct.num = 1;
        direct.f1 = f1;
        direct.f2 = f2;
        flightList.add(direct);

        k++;
      }
      resultSet.close();

      //Find one-hop flights
      if(!directFlight) {
        onehopFlightStatement.clearParameters();

        if (numberOfItineraries - k > 0) {
          onehopFlightStatement.setInt(1, numberOfItineraries - k);
          onehopFlightStatement.setString(2, originCity);
          onehopFlightStatement.setString(3, destinationCity);
          onehopFlightStatement.setInt(4, dayOfMonth);
          ResultSet oneHopResult = onehopFlightStatement.executeQuery();


          while (oneHopResult.next()) {

            Flight f3 = new Flight();
            Flight f4 = new Flight();

            f3.fid = oneHopResult.getInt("f1_fid");
            f3.dayOfMonth = oneHopResult.getInt("f1_day");
            f3.carrierId = oneHopResult.getString("f1_cid");
            f3.flightNum = oneHopResult.getString("f1_num");
            f3.originCity = oneHopResult.getString("f1_origin");
            f3.destCity = oneHopResult.getString("f1_dest");
            f3.time = oneHopResult.getInt("f1_time");
            f3.capacity = oneHopResult.getInt("f1_capacity");
            f3.price = oneHopResult.getInt("f1_price");

            f4.fid = oneHopResult.getInt("f2_fid");
            f4.dayOfMonth = oneHopResult.getInt("f2_day");
            f4.carrierId = oneHopResult.getString("f2_cid");
            f4.flightNum = oneHopResult.getString("f2_num");
            f4.originCity = oneHopResult.getString("f2_origin");
            f4.destCity = oneHopResult.getString("f2_dest");
            f4.time = oneHopResult.getInt("f2_time");
            f4.capacity = oneHopResult.getInt("f2_capacity");
            f4.price = oneHopResult.getInt("f2_price");

            FlightType oneHop = new FlightType();
            oneHop.f1 = f3;
            oneHop.f2 = f4;
            oneHop.num = 2;
            flightList.add(oneHop);
            k++;
          }

          oneHopResult.close();
        }

      }
        if (k == 0) {
          return "No flights match your selection\n";
        }

        int count = 0;
        if (directFlight) {
          for(FlightType f: flightList) {
            sb.append("Itinerary " + count + ": " + f.num + " flight(s), "
                    + f.f1.time + " minutes\n");
            sb.append(f.f1.toString() + "\n");
            count++;
          }
        } else {
          Collections.sort(flightList);
          for(FlightType f: flightList) {
            if(f.f2.carrierId != null) {
              sb.append("Itinerary " + count + ": " + f.num + " flight(s), "
                      + (f.f1.time + f.f2.time) + " minutes\n");
              sb.append(f.f1.toString() + "\n");
              sb.append(f.f2.toString() + "\n");
            } else {
              sb.append("Itinerary " + count + ": " + f.num + " flight(s), "
                      + (f.f1.time) + " minutes\n");
              sb.append(f.f1.toString() + "\n");
            }
            count++;
          }
        }
        return sb.toString();


    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      checkDanglingTransaction();
    }
    return "Failed to search\n";
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in
   *                    the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   *         If the user is trying to book an itinerary with an invalid ID or without having done a
   *         search, then return "No such itinerary {@code itineraryId}\n". If the user already has
   *         a reservation on the same day as the one that they are trying to book now, then return
   *         "You cannot book two flights in the same day\n". For all other errors, return "Booking
   *         failed\n".
   *
   *         And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from 1 and
   *         increments by 1 each time a successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId) {
    try {
      // TODO: YOUR CODE HERE
      return "Booking failed\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n" If the reservation
   *         is not found / not under the logged in user's name, then return "Cannot find unpaid
   *         reservation [reservationId] under user: [username]\n" If the user does not have enough
   *         money in their account, then return "User has only [balance] in account but itinerary
   *         costs [cost]\n" For all other errors, return "Failed to pay for reservation
   *         [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    try {
      // TODO: YOUR CODE HERE
      return "Failed to pay for reservation " + reservationId + "\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    try {
      // TODO: YOUR CODE HERE
      return "Failed to retrieve reservations\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n" For
   *         all other errors, return "Failed to cancel reservation [reservationId]\n"
   *
   *         If successful, return "Canceled reservation [reservationId]\n"
   *
   *         Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId) {
    try {
      // TODO: YOUR CODE HERE
      return "Failed to cancel reservation " + reservationId + "\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Throw IllegalStateException if transaction not completely complete, rollback.
   * 
   */
  private void checkDanglingTransaction() {
    try {
      try (ResultSet rs = tranCountStatement.executeQuery()) {
        rs.next();
        int count = rs.getInt("tran_count");
        if (count > 0) {
          throw new IllegalStateException(
              "Transaction not fully commit/rollback. Number of transaction in process: " + count);
        }
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Database error", e);
    }
  }

  private static boolean isDeadLock(SQLException ex) {
    return ex.getErrorCode() == 1205;
  }

  /**
   * A class to store flight information.
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  /**
   * A class that implements methods to compare two flights
   */
  class FlightType  implements  Comparable<FlightType> {

    public Flight f1;
    public Flight f2;
    public int num;

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(f1.toString());
      if (f2 != null) {
        sb.append(f2.toString());
      }
      return sb.toString();
    };


    @Override
    public int compareTo(FlightType o) {
      if ((this.f1.time + this.f2.time) - (o.f1.time + o.f2.time) == 0) {
        if (this.f1.fid - o.f1.fid == 0) {
          return this.f2.fid - o.f2.fid;
        } else {
          return this.f1.fid - o.f1.fid;
        }
      } else {
        return (this.f1.time + this.f2.time) - (o.f1.time + o.f2.time);
      }
    }
  }

}
