package de.dataelementhub.rest.controller.v1;


import de.dataelementhub.model.dto.export.ExportInfo;
import de.dataelementhub.model.dto.export.ExportRequest;
import de.dataelementhub.model.service.ExportService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
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
  public ExportController(ExportService exportService, Environment env) {
    this.exportService = exportService;
  }

  @Value("${export.exportDirectory}")
  public String exportDirectory;

  @Value("${export.expirationPeriodInDays}")
  private int expirationPeriodInDays;


  /**
   * Return an overview of all exports.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "/export")
  public ResponseEntity<List<ExportInfo>> allExports() {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<ExportInfo> exportDescriptions = exportService.allExports(userId, exportDirectory);
    return new ResponseEntity<List<ExportInfo>>(exportDescriptions, HttpStatus.OK);
  }

  /**
   * Start an export process for all given elements.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(value = "/export")
  public ResponseEntity<String> export(@RequestBody ExportRequest exportRequest,
      @RequestParam(value = "format", required = false, defaultValue = "JSON") String format,
      @RequestParam(value = "fullExport", required = false,
          defaultValue = "true") Boolean fullExport) {
    int numberOfNamespacesExportedFrom = exportRequest.getElementUrns().stream()
        .map(e -> e.split(":")[1]).collect(Collectors.toSet()).size();
    if (numberOfNamespacesExportedFrom > 1) {
      return new ResponseEntity<>("Export from more than one namespace is forbidden",
          HttpStatus.BAD_REQUEST);
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
  @GetMapping(produces={"application/zip", "text/plain"}, value = "/export/{exportId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity exportStatus(
      @PathVariable(value = "exportId") String exportId,
      @RequestParam(value = "onlyUrns", required = false, defaultValue = "false") Boolean onlyUrns)
      throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    ExportInfo exportInfo = exportService.exportInfo(exportId, userId, exportDirectory);
    switch (exportInfo.getStatus()) {
      case "NOT DEFINED":
        return new ResponseEntity<>(exportInfo.toString(), HttpStatus.NOT_FOUND);
      case "DONE":
        String file;
        if (onlyUrns) {
          file = exportDirectory + "/" + userId + "/" + exportId + "-"
              + exportInfo.getFormat().toLowerCase() + "-done/" + "exportedElements.txt";
        } else {
          file = exportDirectory + "/" + userId + "/" + exportId + "-"
              + exportInfo.getFormat().toLowerCase() + "-done/" + exportId + ".zip";
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename="
                + new FileSystemResource(file).getFilename())
            .body(new FileSystemResource(file));
      case "EXPIRED":
        if (onlyUrns) {
          file = exportDirectory + "/" + userId + "/" + exportId + "-"
              + exportInfo.getFormat().toLowerCase() + "-expired/" + "exportedElements.txt";
          return ResponseEntity.ok()
              .header("Content-Disposition", "attachment; filename="
                  + new FileSystemResource(file).getFilename())
              .body(new FileSystemResource(file));
        } else {
          return new ResponseEntity<>("This Export has expired on: " + exportInfo
              .getTimestamp().toLocalDateTime().plusDays(expirationPeriodInDays),
              HttpStatus.NOT_FOUND);
        }
      case "PROCESSING":
        return new ResponseEntity<>(exportInfo.toString(), HttpStatus.ACCEPTED);
      default:
        throw new Exception(String.valueOf(exportService
            .exportInfo(exportId, userId, exportDirectory)));
    }
  }


  /** Delete all expired exports. */
  @Scheduled(fixedRateString = "${export.expiredExportsCheckRate}")
  @PostConstruct
  public void deleteExpiredExports() throws IOException {
    final Instant retentionFilePeriod = ZonedDateTime.now()
        .minusDays(expirationPeriodInDays).toInstant();
    final AtomicInteger countDeletedFiles = new AtomicInteger();
    List<String> parentDirs = new ArrayList<>();
    Files.find(
            Paths.get(exportDirectory),
            6,
        (path, basicFileAttrs) ->
            basicFileAttrs.creationTime().toInstant().isBefore(retentionFilePeriod)
                & path.toString().endsWith(".zip"))
        .forEach(
            fileToDelete -> {
              try {
                if (!Files.isDirectory(fileToDelete)) {
                  Files.delete(fileToDelete);
                  countDeletedFiles.incrementAndGet();
                }
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
              parentDirs.add(fileToDelete.getParent().toString());
            });
    for (String parentDir : parentDirs.stream().distinct().collect(Collectors.toList())) {
      File file = new File(parentDir);
      File newFile = new File(file.getAbsolutePath().replace("-done", "-expired"));
      file.renameTo(newFile);
    }
    countDeletedFiles.get();
  }
}
