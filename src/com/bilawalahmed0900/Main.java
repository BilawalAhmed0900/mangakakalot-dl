package com.bilawalahmed0900;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.image.*;
import java.awt.Graphics;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.geom.AffineTransform;

class URLDownloader
{
    static String download(String URLString)
            throws MalformedURLException, IOException
    {
        URL url = new URL(URLString);
        InputStream IS = url.openStream();
        BufferedReader BIS = new BufferedReader(new InputStreamReader(IS));

        String line;
        StringBuilder result = new StringBuilder();
        while ((line = BIS.readLine()) != null)
        {
            result.append(line);
        }

        BIS.close();
        IS.close();
        return result.toString();
    }
}

class ChapterParser
{
    static String getName(String html_source)
            throws RuntimeException
    {
        String mangaName;
        String patternStringForTitleMangakakakolot = "<span itemprop=\"title\">(.+?)</span>";
        String patternStringForTitleMangakakakolot2 = "<title>(.+?) Manga - Mangakakalot.com</title>";
        String patternStringForTitleManganelo = "<title>(.+?) Manga Online Free - Manganelo</title>";

        Pattern patternTitleMangakakakolot = Pattern.compile(patternStringForTitleMangakakakolot);
        Matcher matcherTitleMangakakakolot = patternTitleMangakakakolot.matcher(html_source);
        
        Pattern patternTitleMangakakakolot2 = Pattern.compile(patternStringForTitleMangakakakolot2);
        Matcher matcherTitleMangakakakolot2 = patternTitleMangakakakolot2.matcher(html_source);
        
        Pattern patternTitleManganelo = Pattern.compile(patternStringForTitleManganelo);
        Matcher matcherTitleManganelo = patternTitleManganelo.matcher(html_source);

        /*
            First gives "Manga Online"
         */
        if (matcherTitleMangakakakolot.find() && matcherTitleMangakakakolot.find())
        {
            // 0 is everything found, 1 is (.*)
            mangaName = matcherTitleMangakakakolot.group(1);
        }
        else if (matcherTitleMangakakakolot2.find())
        {
            mangaName = matcherTitleMangakakakolot2.group(1);
        }
        else if (matcherTitleManganelo.find())
        {
            mangaName = matcherTitleManganelo.group(1);
        }
        else
        {
            throw new RuntimeException("Title(<title></title>) not found");
        }

        return mangaName;
    }

    static ArrayList<String> getChapterLinks(String html_source)
            throws RuntimeException
    {
        ArrayList<String> chapterLinks = new ArrayList<>();

        /*
            ? in regex makes it lazy instead of greedy. If it is not there all chapter links are found in one because
            first one start with <span><a href=" and last one ends in </a></span>
        */
        String patterStringForChapterMangakakakolot = "<span><a href=\"(.+?)\" title=\"(.+?)\">(.+?)</a></span>";
        String patterStringForChapterManganelo = "<a rel=\"nofollow\" class=\"chapter-name text-nowrap\" href=\"(.+?)\" title=\"(.+?)\" title=\"(.+?)\">(.+?)</a>";

        Pattern patterChaptersMangakakakolot = Pattern.compile(patterStringForChapterMangakakakolot);
        Matcher matcherChaptersMangakakakolot = patterChaptersMangakakakolot.matcher(html_source);
        
        Pattern patterChaptersManganelo = Pattern.compile(patterStringForChapterManganelo);
        Matcher matcherChaptersManganelo = patterChaptersManganelo.matcher(html_source);

        boolean found = false;
        while (matcherChaptersMangakakakolot.find())
        {
            /*
                .1, .5 and v2(.2) chapters not included
             */
            /*if (!matcherChapters.group(1).contains(".1") && !matcherChapters.group(1).contains(".2")
                    && !matcherChapters.group(1).contains(".5"))
            {
                chapterLinks.add(matcherChapters.group(1));
            }*/
            chapterLinks.add(matcherChaptersMangakakakolot.group(1));
            found = true;
        }
        
        if (!found)
        {
            while(matcherChaptersManganelo.find())
            {
                chapterLinks.add(matcherChaptersManganelo.group(1));
                found = true;
            }
        }

        if (!found)
        {
            throw new RuntimeException("Chapters not found");
        }

        Collections.reverse(chapterLinks);
        return chapterLinks;
    }

    static ArrayList<String> getImagesLinks(String html_source)
    {
        ArrayList<String> imagesLinks = new ArrayList<>();

        /*
            ? in regex makes it lazy instead of greedy. If it is not there all chapter links are found in one because
            first one start with <span><a href=" and last one ends in </a></span>
        */
        // String patterStringForImages = "<img src=\"(.*?)\"[ ]*alt=\"(.*?)\"[ ]*title=\"(.*?)\"[ ]*/>";
        String patterStringForImages = "https://[a-zA-Z0-9.]+/mangakakalot/[a-zA-Z]+[0-9]+/[a-zA-Z0-9_]+/[a-zA-Z0-9_]+/[0-9]+\\.jpg";

        Pattern patterImages = Pattern.compile(patterStringForImages);
        Matcher matcherImages = patterImages.matcher(html_source);

        boolean found = false;
        while (matcherImages.find())
        {
            imagesLinks.add(matcherImages.group(0));
            found = true;
        }

        if (!found)
        {
            throw new RuntimeException("Images not found");
        }

        return imagesLinks;
    }
}

class Mangakakalot_DL
{
    private String mangaName;
    private int chapterNo;
    private ArrayList<String> chapterLinks;

    Mangakakalot_DL(String html_source)
            throws RuntimeException
    {
        mangaName = ChapterParser.getName(html_source);
        chapterLinks = ChapterParser.getChapterLinks(html_source);
        chapterNo = chapterLinks.size();
    }

    // From 1
    ArrayList<String> getImagesLinks(int chapterNo)
            throws MalformedURLException, IOException
    {
        return ChapterParser.getImagesLinks(URLDownloader.download(chapterLinks.get(chapterNo)));
    }

    int getChapterNo()
    {
        return chapterNo;
    }

    String getMangaName()
    {
        return mangaName;
    }
}

public class Main
{
    private static String DOMAIN_NAME_1 = "mangakakalot.com";
    private static String DOMAIN_NAME_2 = "manganelo.com";

    public static void main(String[] args)
            throws MalformedURLException, IOException, RuntimeException
    {
        for(String arg: args)
        {
            if (arg.contains(DOMAIN_NAME_1) || arg.contains(DOMAIN_NAME_2))
            {
                /*
                    Main class
                 */
                Mangakakalot_DL mangakakalot_dl = new Mangakakalot_DL(URLDownloader.download(arg));

                /*
                    Getting manga name and printing
                 */
                String mangaName = mangakakalot_dl.getMangaName();
                System.out.println("[mangakakalot-dl] Downloading " + mangaName);

                /*
                    Check if the directory for that manga exists, otherwise try to create it
                 */
                File path = new File(mangaName);
                if (!path.isDirectory())
                {
                    boolean hasMade = path.mkdirs();
                    if (!hasMade)
                    {
                        throw new RuntimeException("Cannot make path: \"" + mangaName + "\"");
                    }
                }

                /*
                    Get number of chapters, and loop over them
                 */
                int chapters = mangakakalot_dl.getChapterNo();
                for (int chapter = 0; chapter < chapters; chapter++)
                {
                    /*
                        Previous version
                     */
                    String chapterFileNameCBZ = mangaName + File.separator +
                            "Chapter_" + String.format("%03d", chapter + 1) + ".cbz";
                            
                    /*
                        A jpeg file for it
                     */
                    String chapterFileName = mangaName + File.separator +
                            "Chapter_" + String.format("%03d", chapter + 1) + ".jpeg";
                            
                    File chapterFileCBZ = new File(chapterFileNameCBZ);
                    File chapterFile = new File(chapterFileName);
                    
                    if (chapterFileCBZ.exists() && !chapterFileCBZ.isDirectory())
                    {
                        continue;
                    }
                    if (chapterFile.exists() && !chapterFile.isDirectory())
                    {
                        continue;
                    }

                    /*
                        Getting links of image file for that chapter
                     */
                    ArrayList<String> imageLinks = mangakakalot_dl.getImagesLinks(chapter);
                    ArrayList<BufferedImage> images = new ArrayList<>();
                    for (int imageIndex = 0, max = imageLinks.size(); imageIndex < max; imageIndex++)
                    {
                        try
                        {
                            /*
                                URL and Stream for that url
                             */
                            
                            URL imageURl = new URL(imageLinks.get(imageIndex));
                            InputStream IS = imageURl.openStream();

                            /*
                                Reading from URL and adding to List of BufferedImage's
                             */
                            byte[] dataBuffer = IS.readAllBytes();
                            
                            ByteArrayInputStream bais = new ByteArrayInputStream(dataBuffer);
                            BufferedImage imageBuffer = ImageIO.read(bais);
                            images.add(imageBuffer);   
                        }
                        catch (IOException ignore)
                        {
                            continue;
                        }

                        System.out.printf("[mangakakalot-dl] Downloading Chapter " + (chapter + 1) + "(%06.2f%%)\r",
                                (double)imageIndex / max * 100.0);
                    }
                    
                    /*
                        maxWidth will store width of most wide pic
                        Height will store sum of height of all images
                     */
                    int maxWidth = images.get(0).getWidth();
                    int heigth = 0;
                    for (BufferedImage image: images)
                    {
                        if (image.getWidth() > maxWidth) maxWidth = image.getWidth();
                        heigth += image.getHeight();
                    }
                    
                    double scaleFactor = 1.0f;
                    if (heigth > 65000)
                    {
                        scaleFactor = 65000.0 / (double)heigth;
                        heigth = 65000;
                        maxWidth = (int)((double)maxWidth * scaleFactor);
                        
                        for (int i = 0; i < images.size(); i++)
                        {
                            BufferedImage image = images.get(i);
                            BufferedImage after = new BufferedImage((int)(image.getWidth() * scaleFactor), 
                                (int)(image.getHeight() * scaleFactor), BufferedImage.TYPE_INT_ARGB);
                            AffineTransform at = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
                            AffineTransformOp scaleOp = 
                               new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
                            after = scaleOp.filter(image, after);
                            
                            images.set(i, after);
                            image = null;
                        }
                    }
                    
                    BufferedImage result = new BufferedImage(maxWidth, heigth, BufferedImage.TYPE_INT_RGB);
                    Graphics g = result.getGraphics();
                    
                    int y = 0;
                    for (BufferedImage image: images)
                    {
                        /*
                            Centered draw
                         */
                        g.drawImage(image, (maxWidth - image.getWidth()) / 2, y, image.getWidth(), image.getHeight(), null);
                        y += image.getHeight();
                    }
                    
                    
                    JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
                    jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    jpegParams.setCompressionQuality(1f);
                    
                    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                    writer.setOutput(new FileImageOutputStream(chapterFile));
                    writer.write(null, new IIOImage(result, null, null), jpegParams);
                    
                    System.out.printf("[mangakakalot-dl] Downloading Chapter " + (chapter + 1) + "(100.00%%)\r");
                }
                System.out.println();
            }
            else
            {
                throw new RuntimeException("\"" + arg + "\" is not a valid mangakakalot link");
            }
        }
    }
}
