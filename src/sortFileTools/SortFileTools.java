package sortFileTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Created by pros_cy on 2016/12/22.
 */
public class SortFileTools {
    private final static Properties properties = readProperties();

    private static String[] FILE_SUFFIXS;

    private final static String FILE_NAME_VERSION_REG = "(?:"
            + (Objects.nonNull(properties.getProperty("fileVerisonSplit"))
            && !"".equals(properties.getProperty("fileVerisonSplit"))
            ? properties.getProperty("fileVerisonSplit") : "-")
            + "[0-9\\.]+)|(?:[0-9\\.]+)";

    private final static String FILE_NAME_SPLIT = Objects.nonNull(properties.getProperty("fileNameSplit"))
            && !"".equals(properties.getProperty("fileNameSplit"))
            ? properties.getProperty("fileNameSplit") : "-";

    private final static String SRC_DIR = "srcDir";
    private final static String DEST_DIR = "destDir";
    private final static String INCLUDE_KEYWORD = "includeKeyWord";
    private final static String EXCLUDE_KEYWORD = "excludeKeyWord";
    private final static String JAR_REPEAT_NUM = "jarRepeatNum";
    private final static String KEYWORD_SPLIT = ",";
    private final static String IS_HANDLE_ALL_FILE = "isHandleAllFile";

    public static void main(String[] args) throws Exception {
        String fileSuffix = Objects.nonNull(properties.getProperty("fileSuffix"))
                && !"".equals(properties.getProperty("fileSuffix"))
                ? properties.getProperty("fileSuffix") : ".jar";
        FILE_SUFFIXS = fileSuffix.split(",");

        System.out.println("start sortJar...");
        SortFileTools sortJarTools = new SortFileTools();
        sortJarTools.sortFile();
        System.out.println("sortJar success");
    }

    public void sortFile() throws Exception {
        List<File> files = new ArrayList<>();
        this.loadAllJar(files, properties.getProperty(SRC_DIR));

        String destDir = properties.getProperty(DEST_DIR);
        boolean isHandleAllFile = Objects.nonNull(properties.getProperty(IS_HANDLE_ALL_FILE))
                ? Boolean.parseBoolean(properties.getProperty(IS_HANDLE_ALL_FILE)) : false;

        if (isHandleAllFile) {
            copyAllFile(files, destDir);
        } else {
            copySortedFile(files, destDir);
        }

    }

    private void copySortedFile(List<File> files, String destDir) throws Exception {
        String srcPath = null;
        String destPath = null;
        int jarRepeatNum = Objects.nonNull(properties.getProperty(JAR_REPEAT_NUM))
                ? Integer.parseInt(properties.getProperty(JAR_REPEAT_NUM)) : 2;

        Collection<List<File>> sortFiles = filterFile(files).values();
        Iterator<List<File>> iterator = sortFiles.iterator();

        while (iterator.hasNext()) {
            files = iterator.next();
            orderByFileName(files);
            for (int i = 0, j = files.size(); i < jarRepeatNum && i < j; i++) {
                srcPath = files.get(i).getAbsolutePath();
                destPath = creatSortDir(srcPath, destDir);
                copyJar(srcPath, destPath);
            }
        }
    }

    private void copyAllFile(List<File> files, String destDir) throws Exception {
        String srcPath = null;
        String destPath = null;
        for (File jarFile : files) {
            srcPath = jarFile.getAbsolutePath();
            destPath = creatSortDir(srcPath, destDir);
            copyJar(srcPath, destPath);
        }
    }

    private void orderByFileName(List<File> files) {
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                String o1Name = deleteSuffix(getFileName(o1.getAbsolutePath()));
                String o2Name = deleteSuffix(getFileName(o2.getAbsolutePath()));
                return o2Name.compareTo(o1Name);
            }
        });
    }

    private Map<String, List<File>> filterFile(List<File> files) {
        String includeKeyWord = properties.getProperty(INCLUDE_KEYWORD);
        String excludeKeyWord = properties.getProperty(EXCLUDE_KEYWORD);
        String[] includeKeyWords = {};
        String[] excludeKeyWords = {};
        boolean useInclude = false;
        if (Objects.nonNull(includeKeyWord) && !"".equals(includeKeyWord.trim())) {
            includeKeyWords = includeKeyWord.toUpperCase().split(KEYWORD_SPLIT);
            useInclude = true;
        } else if (!"".equals(excludeKeyWord.trim())) {
            useInclude = false;
            excludeKeyWords = excludeKeyWord.toUpperCase().split(KEYWORD_SPLIT);
        }

        Map<String, List<File>> sortFileMaps = new HashMap<>();
        List<File> fileList = null;
        String keyWord = null;
        String fileName = null;
        for (File jarFile : files) {
            fileName = getFileName(jarFile.getAbsolutePath());
            if ((useInclude && isInclude(fileName, includeKeyWords))
                    || (!useInclude && !isExclude(fileName, excludeKeyWords))) {
                keyWord = getSameFileName(fileName);
                if (sortFileMaps.containsKey(keyWord)) {
                    fileList = sortFileMaps.get(keyWord);
                } else {
                    fileList = new ArrayList<>();
                    sortFileMaps.put(keyWord, fileList);
                }
                fileList.add(jarFile);
            }
        }
        return sortFileMaps;
    }

    private boolean isExclude(String fileName, String[] excludeKeyWords) {
        for (String excludeKeyWord : excludeKeyWords) {
            if (fileName.toUpperCase().contains(excludeKeyWord.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInclude(String fileName, String[] includeKeyWords) {
        if (includeKeyWords.length < 1) return true;
        for (String includeKeyWord : includeKeyWords) {
            if (fileName.toUpperCase().contains(includeKeyWord.trim())) {
                return true;
            }
        }
        return false;
    }

    private void copyJar(String srcPath, String destPath) throws Exception {
        Path path = Paths.get(srcPath);
        OutputStream fileOutputStream = new FileOutputStream(destPath);
        Files.copy(path, fileOutputStream);
    }

    private String creatSortDir(String srcPath, String newDir) {
        String srcFileName = getFileName(srcPath);
        String destDir = genFileNameKeyWord(srcFileName);
        String destDirPath = newDir + File.separator + destDir;
        File dir = new File(destDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return destDirPath + File.separator + srcFileName;
    }

    private String genFileNameKeyWord(String srcFileName) {
        String destDir = deleteSuffix(srcFileName)
                .replaceAll(FILE_NAME_VERSION_REG, "").split(FILE_NAME_SPLIT)[0];
        return destDir;
    }

    private String getSameFileName(String srcFileName) {
        String sameFileName = deleteSuffix(srcFileName)
                .replaceAll(FILE_NAME_VERSION_REG, "");
        return sameFileName;
    }

    private String deleteSuffix(String fileName) {
        for (String fileSuffix : FILE_SUFFIXS) {
            fileName = fileName.replace(fileSuffix, "");
        }
        return fileName;
    }

    private String getFileName(String srcPath) {
        int index = srcPath.lastIndexOf(File.separator);
        String fileName = srcPath.substring(index + 1, srcPath.length());
        return fileName;
    }

    private void loadAllJar(List<File> files, String rootDir) {
        File srcDirFile = new File(rootDir);
        File[] srcDirs = srcDirFile.listFiles();
        for (File srcDir : srcDirs) {
            if (srcDir.isFile() && endsWithSuffix(srcDir.getName())) {
                files.add(srcDir);
            } else if (srcDir.isDirectory()) {
                loadAllJar(files, srcDir.getAbsolutePath());
            }
        }
    }

    private boolean endsWithSuffix(String fileName) {
        for (String fileSuffix : FILE_SUFFIXS) {
            if (fileName.endsWith(fileSuffix)) {
                return true;
            }
        }
        return false;
    }

    private static Properties readProperties() {
        Properties properties = new Properties();
        try {
            InputStream inputStream = new FileInputStream("info.properties");
            properties.load(inputStream);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return properties;
    }
}
