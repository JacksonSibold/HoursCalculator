import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BillPlanner {

    // Class representing a Bill
    public static class Bill {
        String date;       // Date grabbed from the web (or fallback)
        String name;
        double cost;
        boolean recurring;
        String frequency;  // "monthly" or "yearly" (if recurring)

        public Bill(String date, String name, double cost, boolean recurring, String frequency) {
            this.date = date;
            this.name = name;
            this.cost = cost;
            this.recurring = recurring;
            this.frequency = frequency;
        }

        // Convert bill information to a CSV record (comma separated)
        public String toCSV() {
            return date + "," + name + "," + cost + "," + recurring + "," + frequency;
        }

        // Create a Bill object from a CSV line
        public static Bill fromCSV(String line) {
            String[] parts = line.split(",");
            if (parts.length < 5)
                return null;
            String date = parts[0];
            String name = parts[1];
            double cost = Double.parseDouble(parts[2]);
            boolean recurring = Boolean.parseBoolean(parts[3]);
            String frequency = parts[4];
            return new Bill(date, name, cost, recurring, frequency);
        }
    }

    /**
     * Fetches the current date/time from a web API.
     * Connects to WorldTimeAPI for the America/Denver timezone.
     * If there is an error, falls back to the local system time.
     */
    public static String getCurrentDateFromWeb() {
        String apiUrl = "http://worldtimeapi.org/api/timezone/America/Denver";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            String json = content.toString();
            int idx = json.indexOf("\"datetime\":\"");
            if (idx != -1) {
                int start = idx + "\"datetime\":\"".length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    String datetime = json.substring(start, end);
                    return datetime;
                }
            }
        } catch (Exception e) {
            return LocalDateTime.now().toString();}
        // Fallback to system date/time
        return LocalDateTime.now().toString();
    }

    // Saves the list of bills into a CSV file.
    public static void saveBillsToCSV(List<Bill> bills, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            // Write header
            pw.println("Date,BillName,Cost,Recurring,Frequency");
            for (Bill b : bills) {
                pw.println(b.toCSV());
            }
        } catch (IOException e) {
            System.out.println("Error saving to file: " + e.getMessage());
        }
    }

    // Loads bill records from a CSV file.
    public static List<Bill> loadBillsFromCSV(String filename) {
        List<Bill> bills = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                Bill b = Bill.fromCSV(line);
                if (b != null)
                    bills.add(b);
            }
        } catch (IOException e) {
            System.out.println("Error loading from file: " + e.getMessage());
        }
        return bills;
    }

    /**
     * Generates a backup file name based on the current date.
     * The format is "MM-dd-yy.csv". If a file with the name already exists,
     * the method appends a version number (e.g., "_v2") and increases the version
     * until an unused file name is found.
     */
    public static String generateBackupFileName() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy");
        String baseName = today.format(formatter);
        String filename = baseName + ".csv";
        File file = new File(filename);
        int version = 2;
        while (file.exists()) {
            filename = baseName + "_v" + version + ".csv";
            file = new File(filename);
            version++;
        }
        return filename;
    }

    /**
     * Calculates the weekly hours needed given the user's hourly rate,
     * the required monthly net amount (bill costs), and the tax rate.
     * Uses standard hourly rate for the first 40 hours and 1.5x pay for overtime.
     */
    public static double computeWeeklyHours(double hourlyRate, double monthlyCost, double taxRate) {
        // Calculate required gross monthly income (before taxes)
        double requiredGross = monthlyCost / (1 - taxRate);
        // Estimate weekly income needed (assuming 4 weeks per month)
        double weeklyIncomeNeeded = requiredGross / 4;
        double weeklyNeededNoOvertime = weeklyIncomeNeeded / hourlyRate;
        if (weeklyNeededNoOvertime <= 40) {
            return weeklyNeededNoOvertime;
        } else {
            // Calculate overtime hours:
            // Weekly income = (40 * rate) + ((h - 40) * rate * 1.5)
            double overtimeIncomeNeeded = weeklyIncomeNeeded - (40 * hourlyRate);
            double overtimeHours = overtimeIncomeNeeded / (hourlyRate * 1.5);
            return 40 + overtimeHours;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Bill> bills = new ArrayList<>();

        System.out.println("Welcome to the Bill Planner!");
        System.out.println("Please select an option:");
        System.out.println("1. Load existing bills file");
        System.out.println("2. Create a new bills file");
        int option = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (option == 1) {
            System.out.print("Enter the file name to load: ");
            String filename = scanner.nextLine();
            bills = loadBillsFromCSV(filename);
            if (bills.size() > 0) {
                System.out.println("Loaded " + bills.size() + " bills from file.");
            } else {
                System.out.println("No bills loaded. Starting with an empty list.");
            }
        } else if (option == 2) {
            System.out.println("Creating a new bills file.");
        } else {
            System.out.println("Invalid option. Exiting.");
            scanner.close();
            return;
        }

        // Allow the user to add new bills.
        while (true) {
            System.out.println("Would you like to add a new bill? (yes/no)");
            String response = scanner.nextLine().trim();
            if (!(response.equalsIgnoreCase("yes")||response.equalsIgnoreCase("y")))
                break;

            System.out.print("Enter Bill Name: ");
            String name = scanner.nextLine();
            System.out.print("Enter Bill Cost (in dollars): ");
            double cost = scanner.nextDouble();
            scanner.nextLine(); // Consume newline

            System.out.print("Is this a recurring bill? (yes/no): ");
            String rec = scanner.nextLine().trim();
            boolean recurring = rec.equalsIgnoreCase("yes") || rec.equalsIgnoreCase("y");

            String frequency = "";
            if (recurring) {
                System.out.print("Is it monthly or yearly? ");
                frequency = scanner.nextLine().trim();
            }

            // Get the current date from the web (or fallback)
            String date = getCurrentDateFromWeb();
            Bill newBill = new Bill(date, name, cost, recurring, frequency);
            bills.add(newBill);
        }

        // Compute bill totals:
        // - For recurring bills: use cost as-is for monthly, or prorate yearly costs by dividing by 12.
        // - Non-recurring bills are added directly.
        double totalRecurring = 0;
        double totalNonRecurring = 0;
        for (Bill b : bills) {
            if (b.recurring) {
                if (b.frequency.equalsIgnoreCase("monthly")) {
                    totalRecurring += b.cost;
                } else if (b.frequency.equalsIgnoreCase("yearly")) {
                    totalRecurring += b.cost;
                }
            } else {
                totalNonRecurring += b.cost;
            }
        }
        double totalMonthly = totalRecurring + totalNonRecurring;

        // Ask for the user's hourly rate.
        System.out.print("Enter your hourly rate (in dollars): ");
        double hourlyRate = scanner.nextDouble();

        // Define tax rates for Colorado:
        // Colorado state tax is around 4.55% and assume an effective federal rate of 12%
        double taxRate = 0.1876; // Combined effective tax rate 

        // Calculate the required weekly hours (assuming 4 weeks per month).
        double weeklyHoursNeeded = computeWeeklyHours(hourlyRate, totalMonthly, taxRate);

        // Print bill summary and required work hours.
        System.out.println("\n----- Bill Summary -----");
        System.out.println("Total Monthly Bill Cost: $" + String.format("%.2f", totalMonthly));
        System.out.println("Recurring Bills Total : $" + String.format("%.2f", totalRecurring));
        System.out.println("Non-Recurring Bills Total: $" + String.format("%.2f", totalNonRecurring));
        System.out.println("To cover these bills after taxes, you need to work approximately " 
                + String.format("%.2f", weeklyHoursNeeded) + " hours per week.");

        // Prompt to save the backup without asking a file name;
        // the file name is generated automatically in the desired format.
        
        
        String filename = generateBackupFileName();
        saveBillsToCSV(bills, filename);
        System.out.println("Bills saved to " + filename);
        

        scanner.close();
    }
}
