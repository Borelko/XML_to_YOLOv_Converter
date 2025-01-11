import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.lang.Math.abs;
import java.nio.file.Paths;
import java.util.Objects;

public class Converter {

    public static void main(String[] args) {

        String Indir = "E:/..."; // Input directory with .xml and .jpg files
        String OutDir = "E:/..."; //Path to Output folder
        String Tag = "..."; // Name pattern of output file
        // For example if Tag = person_on_boat
        // In OutDir/images will be person_on_boat_X.jpg images
        // In OutDir/labels will be person_on_boat_X.txt files

        int min_width_of_box = 15; // Minimum width of object
        int min_height_of_box = 15; // Minimum height of object

        File[] files = new File(Indir).listFiles(); // Scan files into Indir

        CreateFolder(OutDir); //Create OutDir directory

        System.out.println("Start converting...");

        for (int num = 0; num < Objects.requireNonNull(files).length - 1; num += 2) {

            File IMGFILE = files[num];
            File XMLFILE = files[num + 1];
            String To_Path_label = OutDir + "/labels/" + Tag + num + ".txt"; // (for example C:/labled/Train/labels/test_(number).txt)
            String To_Path_image = OutDir + "/images/" + Tag + num + ".jpg"; // (for example C:/labled/Train/images/test_(number).jpg)

            if (!IMGFILE.exists()) continue;  // Check for existing xml file
            if (!XMLFILE.exists()) continue;  // Check for existing jpg file

            Reading_and_Writing(XMLFILE, IMGFILE, To_Path_label, To_Path_image, min_width_of_box, min_height_of_box);

        }
        System.out.println("Converting finished!");
    }

    public static String Change_class(String clas) {
        switch (clas) {
            case "human" -> clas = clas.replace("human", "0");
            case "person" -> clas = clas.replace("person", "0");
            case "bicycle" -> clas = clas.replace("bicycle", "1");
            case "car" -> clas = clas.replace("car", "2");
            case "motorcycle" -> clas = clas.replace("motorcycle", "3");
            case "bus" -> clas = clas.replace("bus", "4");
            case "truck" -> clas = clas.replace("truck", "5");
            case "boat" -> clas = clas.replace("boat", "6");
            case "dog" -> clas = clas.replace("dog", "7");
            // Add here your case or modify created it's up to u

            default -> clas = "123";
        }
        return clas;
    }

    public static void Reading_and_Writing(File file, File img, String To_Path_label, String To_Path_image, int min_width_of_box, int min_height_of_box) {

        try (FileWriter writer = new FileWriter(To_Path_label)) {

            BufferedImage bimg = ImageIO.read(img);
            int r_width = bimg.getWidth();
            int r_height = bimg.getHeight();

            moveFile(String.valueOf(img), To_Path_image);

            NodeList list = GET_NODELIST_FROM_XML(file);

            for (int i = 0; i < list.getLength(); i++) {

                Node node = list.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node.getChildNodes();

                    String clas = element.getElementsByTagName("name").item(0).getTextContent();
                    String xmin = element.getElementsByTagName("xmin").item(0).getTextContent();
                    String ymin = element.getElementsByTagName("ymin").item(0).getTextContent();
                    String xmax = element.getElementsByTagName("xmax").item(0).getTextContent();
                    String ymax = element.getElementsByTagName("ymax").item(0).getTextContent();
                    String ccls = Change_class(clas);

                    int klas = Integer.parseInt(ccls);
                    double x1 = Double.parseDouble(xmin);
                    double y1 = Double.parseDouble(ymin);
                    double x2 = Double.parseDouble(xmax);
                    double y2 = Double.parseDouble(ymax);

                    double x_center = ((x2 + x1) / 2);
                    double y_center = ((y2 + y1) / 2);
                    double width = (abs(x2 - x1));
                    double height = (abs(y2 - y1));

                    if (klas != 123) {
                        if (width > min_width_of_box && height > min_height_of_box)
                            if (((x_center / r_width) > 0) && ((y_center / r_height) > 0) && ((width / r_width) > 0) && ((height / r_height) > 0) && ((x_center / r_width) <= 1) && ((y_center / r_height) <= 1) && ((width / r_width) <= 1) && ((height / r_height) <= 1)) {
                                writer.write(klas + " ");
                                writer.write((x_center / r_width) + " ");
                                writer.write((y_center / r_height) + " ");
                                writer.write(((width) / r_width) + " ");
                                writer.write(((height) / r_height) + " ");
                                writer.write(System.lineSeparator());
                            }
                    } else {
                        System.out.println(clas);
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public static NodeList GET_NODELIST_FROM_XML(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(String.valueOf(file)));
        doc.getDocumentElement().normalize();
        return doc.getElementsByTagName("object");
    }

    private static void CreateFolder(String OutDir) {
        try {

            Path pathlabels = Paths.get(OutDir + "/labels");
            Files.createDirectories(pathlabels);
            System.out.println("Directory " + pathlabels + " is created!");

            Path pathimages = Paths.get(OutDir + "/images");
            Files.createDirectories(pathimages);
            System.out.println("Directory " + pathimages + " is created!");

        } catch (IOException e) {

            System.err.println("Failed to create directory!" + e.getMessage());

        }
    }

    private static void moveFile(String src, String dest) {
        Path result = null;
        try {
            result = Files.move(Paths.get(src), Paths.get(dest));
        } catch (IOException e) {
            System.out.println("Exception while moving file: " + e.getMessage());
        }
        if (result != null) {
            System.out.println("File movement from" + src + " To " + dest + "   Success");
        } else {
            System.out.println("File movement from" + src + " To " + dest + "  Failed.");
        }
    }
}
