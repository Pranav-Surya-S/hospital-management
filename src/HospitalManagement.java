import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class HospitalManagement {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Connection connection = DbConnection.getConnection();
        
        if (connection == null) {
            System.out.println("Failed to connect to Database.");
            return;
        }

        System.out.println("--- HOSPITAL MANAGEMENT SYSTEM LOGIN ---");
        // Login Logic
        String userRole = login(connection, scanner);
        
        if (userRole == null) {
            System.out.println("Invalid Username or Password. Exiting.");
            return;
        }

        System.out.println("Login Successful! Welcome, " + userRole);

        Patient patient = new Patient(connection, scanner);
        Doctors doctor = new Doctors(connection, scanner);

        while (true) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Add Patient");
            System.out.println("2. View Patients");
            System.out.println("3. View Doctors");
            System.out.println("4. Book Appointment");
            
            // Only Admin can add doctors
            if (userRole.equals("ADMIN")) {
                System.out.println("5. Add Doctor (ADMIN ONLY)");
            }
            
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            
            int choice = -1;
            if(scanner.hasNextInt()) {
                choice = scanner.nextInt();
            } else {
                scanner.next(); // Clear invalid input
            }

            switch (choice) {
                case 1:
                    patient.addPatient();
                    break;
                case 2:
                    patient.viewPatients();
                    break;
                case 3:
                    doctor.viewDoctors();
                    break;
                case 4:
                    bookAppointment(patient, doctor, connection, scanner);
                    break;
                case 5:
                    if (userRole.equals("ADMIN")) {
                        doctor.addDoctor();
                    } else {
                        System.out.println("Access Denied.");
                    }
                    break;
                case 0:
                    System.out.println("Exiting System.");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // Login Method
    private static String login(Connection connection, Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.next();
        System.out.print("Password: ");
        String password = scanner.next();

        String query = "SELECT role FROM users WHERE username = ? AND password = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("role"); // Returns 'ADMIN' or 'USER'
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void bookAppointment(Patient patient, Doctors doctor, Connection connection, Scanner scanner) {
        System.out.print("Enter Patient ID: ");
        int patientId = scanner.nextInt();
        System.out.print("Enter Doctor ID: ");
        int doctorId = scanner.nextInt();
        System.out.print("Enter Appointment Date (YYYY-MM-DD): ");
        String appointmentDate = scanner.next();

        if (patient.getPatientById(patientId) && doctor.getDoctorById(doctorId)) {
            if (checkDoctorAvailability(doctorId, appointmentDate, connection)) {
                String query = "INSERT INTO appointments(patient_id, doctor_id, appointment_date) VALUES(?, ?, ?)";
                try {
                    PreparedStatement ps = connection.prepareStatement(query);
                    ps.setInt(1, patientId);
                    ps.setInt(2, doctorId);
                    ps.setString(3, appointmentDate);
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Appointment Booked!");
                    } else {
                        System.out.println("Booking Failed.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Doctor not available on this date.");
            }
        } else {
            System.out.println("Invalid Patient or Doctor ID.");
        }
    }

    public static boolean checkDoctorAvailability(int doctorId, String date, Connection connection) {
        String query = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, doctorId);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count == 0; // Available if count is 0
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}