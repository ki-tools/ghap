package io.ghap.visualization.publish.data.validation;

import com.google.common.io.Files;
import com.google.gson.Gson;
import org.codehaus.plexus.archiver.tar.GZipTarFile;
import org.codehaus.plexus.archiver.tar.TarEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ManifestValidator {
    private static Logger log = LoggerFactory.getLogger(ManifestValidator.class);

    final static Set<String> extensionSet = new HashSet<>(Arrays.asList("png", "jpg", "jpeg"));

    final static String META_DATA = "meta-data.json";
    final static String APPLICATION_NAME = "ApplicationName";
    final static String APPLICATION_DESCRIPTION = "Description";
    final static String APPLICATION_AUTHOR = "Author";
    final static String APPLICATION_PROJECT = "Project";
    final static String APPLICATION_GRANT = "Grant";
    final static String APPLICATION_KEYWORDS = "Keyword";
    final static String APPLICATION_THUMBNAIL = "Thumbnail";
    final static String APPLICATION_ROOT = "ApplicationRoot";
    final static String APPLICATION_UPDATE = "Updated";

    private static List<String> nullCheckFields = Arrays.asList(
            APPLICATION_NAME,
            APPLICATION_AUTHOR,
            APPLICATION_ROOT,
            APPLICATION_DESCRIPTION,
            APPLICATION_PROJECT,
            APPLICATION_GRANT,
            APPLICATION_THUMBNAIL,
            APPLICATION_KEYWORDS);

    private Map<String, Object> metaData;

    private Set<String> files = new HashSet<>();
    List<ValidationError> errors = new ArrayList(0);


    public ManifestValidator(GZipTarFile zipFile, String meta) throws IOException {
        Gson gson = new Gson();

        if(meta != null && !(meta = meta.trim()).isEmpty()){
            log.debug("The following meta-data was provided: " + meta);
            this.metaData = gson.fromJson(meta, HashMap.class);
        }
        // collect app map
        Enumeration entries = zipFile.getEntries();
        Path root = null;
        while (entries.hasMoreElements()) {
            TarEntry entry = (TarEntry) entries.nextElement();
            File f = new File(entry.getName());

            if(root == null){
                root = getRoot(f).toPath();
            }

            if( f.getName().equals(META_DATA) && this.metaData == null){
                try(InputStream is = zipFile.getInputStream(entry)){
                    this.metaData = gson.fromJson(new InputStreamReader(is, UTF_8), HashMap.class);
                }
            }
            else if( isValidatable(f) ){
                Path path = root.relativize(f.toPath());
                files.add(path.normalize().toString());
            }

            if(this.metaData != null && !this.metaData.containsKey("ApplicationRoot")){
                this.metaData.put("ApplicationRoot", root.toFile().getName());
                log.debug("Set ApplicationRoot to \"" + this.metaData.get("ApplicationRoot") + "\"");
            }
        }

    }

    public Map<String, ?> getMetaData() {
        return metaData;
    }

    public List<ValidationError> validate(){
        if( metaData == null ){
            addError(META_DATA, "is_missing", "is missing");
        }
        else {
            // check for null
            for( Map.Entry<String,  ?> entry:metaData.entrySet() ){
                String key = entry.getKey();
                if( nullCheckFields.contains(key) ){
                    if( entry.getValue() == null ){
                        // add error
                        addError(key, "is_missing", "is missing");
                    }
                }
            }

            // check thumbnails extension
            String applicationThumbnail = (String) metaData.get(APPLICATION_THUMBNAIL);
            if(applicationThumbnail != null) {
                String ext = Files.getFileExtension(applicationThumbnail);
                if( !extensionSet.contains(ext) ){
                    // add error
                    addError(APPLICATION_THUMBNAIL, "wrong_extension", "has the wrong file extension, expected extension of type png, jpg, or jpeg");
                }
                else {
                    // check thumbnails presents
                    String normPath = new File(applicationThumbnail).toPath().normalize().toString();
                    if( !files.contains(normPath) ){
                        // add error
                        addError(APPLICATION_THUMBNAIL, "wrong_ref", "doesn't exist in the application archive");
                    }
                }
            }

        }
        return errors.isEmpty() ? null:errors;
    }

    private static File getRoot(File f){
        return f.getParent() == null ? f:getRoot(f.getParentFile());
    }

    private static boolean isValidatable(File path) {
        String ext = Files.getFileExtension(path.getName());
        return extensionSet.contains(ext);
    }

    private void addError(final String field, final String code, final String message){
        for(ValidationError error:errors){
            if( error.getField().equalsIgnoreCase(field) ){
                error.getErrors().add(error.new Error(code, message));
                return;
            }
        }

        errors.add(new ValidationError(field, code, message));
    }
}
