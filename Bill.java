public class Bill {
    String date;
    String name;
    double cost;
    boolean recurring;
    String frequency;

    public Bill(String date, String name, double cost, boolean recurring, String frequency) {
        this.date = date;
        this.name = name;
        this.cost = cost;
        this.recurring = recurring;
        this.frequency = frequency;
    }

    public String toCSV() {
        return date + "," + name + "," + cost + "," + recurring + "," + frequency;
    }

    public static Bill fromCSV(String line) {
        String[] parts = line.split(",");
        if (parts.length < 5) return null;
        String date = parts[0];
        String name = parts[1];
        double cost = Double.parseDouble(parts[2]);
        boolean recurring = Boolean.parseBoolean(parts[3]);
        String frequency = parts[4];
        return new Bill(date, name, cost, recurring, frequency);
    }
}
