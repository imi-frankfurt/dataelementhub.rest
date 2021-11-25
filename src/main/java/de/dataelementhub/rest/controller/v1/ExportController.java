package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.model.dto.export.ExportInfo;
import de.dataelementhub.model.dto.export.ExportRequest;
import de.dataelementhub.model.service.ExportService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class ExportController {

  private final ExportService exportService;

  @Autowired
  public ExportController(ExportService exportService) {
    this.exportService = exportService;
  }

  public static String exportDirectory = System.getProperty("user.dir") + "/uploads/export/";

  /**
   * Return an overview of all exports.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "/export")
  public ResponseEntity<List<ExportInfo>> allExports() {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<ExportInfo> exportDescriptions = exportService.allExports(userId, exportDirectory);
    return new ResponseEntity<List<ExportInfo>>(exportDescriptions, HttpStatus.ACCEPTED);
  }

  /**
   * Start an export process for all given elements.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(value = "/export")
  public ResponseEntity<String> export(@RequestBody ExportRequest exportRequest,
      @RequestParam String format, @RequestParam Boolean fullExport) {
    int numberOfNamespacesExportedFrom = exportRequest.getElementUrns().stream()
        .map(e -> e.split(":")[1]).collect(Collectors.toSet()).size();
    if (numberOfNamespacesExportedFrom > 1) {
      return new ResponseEntity<>("Export from more than one namespace is forbidden",
          HttpStatus.NOT_ACCEPTABLE);
    }
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    String timestamp = new Timestamp(System.currentTimeMillis())
        .toString().replaceAll("[ \\.\\-\\:]", "_");
    exportService
        .exportService(exportRequest, userId, format, fullExport, timestamp, exportDirectory);
    return new ResponseEntity<>(timestamp, HttpStatus.ACCEPTED);
  }


  /**
   * Returns export file if done otherwise returns export status.
   */
  @GetMapping(produces = "application/zip", value = "/export/{exportId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity exportStatus(
      @PathVariable(value = "exportId") String exportId)
      throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    ExportInfo exportInfo = exportService.exportInfo(exportId, userId, "export");
    switch (exportInfo.getStatus()) {
      case "NOT DEFINED":
        return new ResponseEntity<>(exportInfo.toString(), HttpStatus.NOT_ACCEPTABLE);
      case "DONE":
        String file = System.getProperty("user.dir")
            + "/uploads/export/" + userId + "/" + exportId + "-"
            + exportInfo.getFormat().toLowerCase() + "-done/" + exportId + ".zip";
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=" + "export.zip")
            .body(new FileSystemResource(file));
      case "PROCESSING":
        return new ResponseEntity<>(exportInfo.toString(), HttpStatus.ACCEPTED);
      default:
        throw new Exception(String.valueOf(exportService.exportInfo(exportId, userId, "export")));
    }
  }
}
