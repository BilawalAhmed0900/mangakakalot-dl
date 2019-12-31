package com.bilawalahmed0900;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        String patternStringForTitleManganelo = "<title>(.+?) Manga Online Free - Manganelo</title>";

        Pattern patternTitleMangakakakolot = Pattern.compile(patternStringForTitleMangakakakolot);
        Matcher matcherTitleMangakakakolot = patternTitleMangakakakolot.matcher(html_source);
        
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
                        A CBZ for that chapter, file and temporary(while downloading) file for it
                     */
                    String chapterFileName = mangaName + File.separator +
                            "Chapter_" + String.format("%03d", chapter + 1) + ".cbz";
                    File chapterFile = new File(chapterFileName);
                    if (chapterFile.exists() && !chapterFile.isDirectory())
                    {
                        continue;
                    }

                    File chapterFileTemp = new File(chapterFileName + "__temp");
                    if (chapterFile.exists() && !chapterFile.isDirectory())
                    {
                        if (!chapterFileTemp.delete())
                        {
                            throw new RuntimeException("Cannot delete \"" + chapterFileName + "__temp" + "\"");
                        }
                    }

                    /*
                        A CBZ is a zip file
                     */
                    ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(chapterFileName + "__temp"));

                    /*
                        Getting links of image file for that chapter
                     */
                    ArrayList<String> imageLinks = mangakakalot_dl.getImagesLinks(chapter);
                    for (int imageIndex = 0, max = imageLinks.size(); imageIndex < max; imageIndex++)
                    {
                        /*
                            A jpg name for that panel
                         */
                        String imageName = String.format("%03d", imageIndex + 1) + ".jpg";

                        /*
                            Making an entry of that file in zip
                         */
                        zipFile.putNextEntry(new ZipEntry(imageName));

                        /*
                            URL and Stream for that url
                         */
                        URL imageURl = new URL(imageLinks.get(imageIndex));
                        InputStream IS = imageURl.openStream();

                        /*
                            Reading from URL and writing to zip
                         */
                        byte[] dataBuffer = IS.readAllBytes();
                        zipFile.write(dataBuffer);
                        zipFile.closeEntry();

                        System.out.printf("[mangakakalot-dl] Downloading Chapter " + (chapter + 1) + "(%.2f%%)\r",
                                (double)imageIndex / max * 100.0);
                    }

                    zipFile.close();

                    File f1 = new File(chapterFileName + "__temp");
                    File f2 = new File(chapterFileName);
                    if (!f1.renameTo(f2))
                    {
                        throw new RuntimeException("Cannot rename files from \"" + chapterFileName + "__temp" + "\" to \"" + chapterFileName + "\"");
                    }
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
