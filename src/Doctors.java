import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Doctors {
    private Connection connection;
    private Scanner scanner;

    public Doctors(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
    }

    public void viewDoctors() {
        String query = "SELECT * FROM doctors";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            System.out.println("Doctors:");
            System.out.println("+--------+----------------------+--------------------+");
            System.out.println("|   ID   |      Doctor Name     |   Specialization   |");
            System.out.println("+--------+----------------------+--------------------+");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String specialization = resultSet.getString("specialization");
                System.out.printf("|%-8s|%-22s|%-20s|\n", id, name, specialization);
                System.out.println("+--------+----------------------+--------------------+");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDoctor() {
        System.out.print("Enter Doctor Name: ");
        scanner.nextLine(); // Consume leftover newline
        String name = scanner.nextLine();
        System.out.print("Enter Specialization: ");
        String specialization = scanner.nextLine();

        String query = "INSERT INTO doctors(name, specialization) VALUES(?, ?)";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, specialization);
            int rows = preparedStatement.executeUpdate();
            if (rows > 0) {
                System.out.println("Doctor added successfully!");
            } else {
                System.out.println("Failed to add doctor.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean getDoctorById(int id) {
        String query = "SELECT * FROM doctors WHERE id = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}