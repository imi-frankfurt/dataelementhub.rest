package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.jooq.enums.GrantType;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.importexport.ExportDescription;
import de.dataelementhub.model.dto.importexport.ExportDto;
import de.dataelementhub.model.dto.importexport.ImportDescription;
import de.dataelementhub.model.service.ImportExportService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1")
public class ImportExportController {

  private final ImportExportService importExportService;

  @Autowired
  public ImportExportController(ImportExportService importExportService) {
    this.importExportService = importExportService;
  }

  public static String importDirectory = System.getProperty("user.dir") + "/uploads/import/";
  public static String exportDirectory = System.getProperty("user.dir") + "/uploads/export/";

  /**
   * .
   * @return
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "/import")
  public ResponseEntity<List<ImportDescription>> allImports() throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<ImportDescription> importDescriptions = importExportService.allImports(userId, importDirectory);
    return new ResponseEntity<List<ImportDescription>>(importDescriptions, HttpStatus.ACCEPTED);
  }

  /**
   * .
   * @return
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "/export")
  public ResponseEntity<List<ExportDescription>> allExports() throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<ExportDescription> exportDescriptions = importExportService.allExports(userId, exportDirectory);
    return new ResponseEntity<List<ExportDescription>>(exportDescriptions, HttpStatus.ACCEPTED);
  }

  /**
   * Upload import files to server and return import id.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(consumes = {"multipart/form-data", "application/json"}, value = "/import")
  public ResponseEntity<String> importFiles(@RequestBody List<MultipartFile> file,
                            @RequestParam("namespaceUrn") String namespaceUrn) throws Exception {
    String timestamp =
            new Timestamp(System.currentTimeMillis())
                .toString().replaceAll("[ \\.\\-\\:]", "_");
    Integer namespaceIdentifier = Integer.valueOf(namespaceUrn.split(":")[1]);
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    if (!DaoUtil.checkGrants(namespaceIdentifier, userId,
        Arrays.asList(GrantType.ADMIN, GrantType.WRITE))) {
      return new ResponseEntity<>(
          "Only users with WRITE or ADMIN Grant can import to this namespace.",
          HttpStatus.UNAUTHORIZED);
    }
    importExportService.importService(file, namespaceUrn, importDirectory, userId, timestamp);
    return new ResponseEntity<>(timestamp, HttpStatus.ACCEPTED);
  }


  /**
   * check import status and return it.
   */
  @GetMapping(value = "/import/{importId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity<String> importStatus(
      @PathVariable(value = "importId") String importId) throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    switch (importExportService.checkStatus(importId, userId, "import").toUpperCase()) {
      case "NOT DEFINED":
        return new ResponseEntity<>("Import ID: "
            + importId + " is not defined!", HttpStatus.NOT_ACCEPTABLE);
      case "DONE":
        return new ResponseEntity<>(HttpStatus.OK);
      case "PROCESSING":
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      default:
        throw new Exception(importExportService.checkStatus(importId, userId, "import"));
    }
  }

  /**
   * Export elements in XML/JSON format and send it as Zip.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(value = "/export")
  public ResponseEntity<String> export(@RequestBody ExportDto exportDto,
      @RequestParam String format, @RequestParam Boolean fullExport) throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String timestamp =
            new Timestamp(System.currentTimeMillis()).toString().replaceAll("[ \\.\\-\\:]", "_");
//    if (auth != null && auth.getAuthorities().stream()
//        .anyMatch(a -> a.getAuthority().equals("SCOPE_importExport"))) {
      importExportService.exportService(exportDto, userId, format, fullExport, timestamp, exportDirectory);
      return new ResponseEntity<>(timestamp, HttpStatus.ACCEPTED);
//    } else {
//      return new ResponseEntity<>("Import and Export access is not granted.", HttpStatus.FORBIDDEN);
//    }
  }


  /**
   * Returns export file if done otherwise returns export status.
   */
  @GetMapping(produces = "application/zip", value = "/export/{exportId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity<FileSystemResource> exportStatus(
      @PathVariable(value = "exportId") String exportId)
          throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    String file = System.getProperty("user.dir")
        + "/uploads/export/" + userId + "/" + exportId + "/" + exportId + ".zip";
    switch (importExportService.checkStatus(exportId, userId, "export").toUpperCase()) {
      case "NOT DEFINED":
        return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
      case "DONE":
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + "export.zip")
                .body(new FileSystemResource(file));
      case "PROCESSING":
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      default:
        throw new Exception(importExportService.checkStatus(exportId, userId, "export"));
    }
  }
}
