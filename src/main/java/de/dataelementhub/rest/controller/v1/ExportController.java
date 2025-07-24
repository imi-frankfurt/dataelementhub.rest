package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.model.dto.importexport.ExportInfo;
import de.dataelementhub.model.dto.importexport.ExportRequest;
import de.dataelementhub.model.handler.UserHandler;
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
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Export Controller.
 */
@Transactional
@RestController
@RequestMapping("/" + ApiVersion.API_VERSION)
public class ExportController {

  public static final String EXPORTED_ELEMENTS_FILENAME = "exportedElements.txt";
  public static final String FORMAT_PARAM = "format";
  public static final String FULL_EXPORT_PARAM = "fullExport";
  private final ExportService exportService;

  private final DSLContext ctx;

  @Autowired
  public ExportController(ExportService exportService, DSLContext ctx) {
    this.exportService = exportService;
    this.ctx = ctx;
  }


  /**
   * Return an overview of all exports.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "/export")
  public ResponseEntity<List<ExportInfo>> allExports() {
    Integer userId = UserHandler.getUserByIdentity(ctx,
        DataElementHubRestApplication.getCurrentUserName()).getId();
    List<ExportInfo> exportDescriptions = exportService
        .allExports(userId, exportService.getExportDirectory());
    return new ResponseEntity<>(exportDescriptions, HttpStatus.OK);
  }

  /**
   * Start an export process for all given elements.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(value = "/export")
  public ResponseEntity<String> export(@RequestBody ExportRequest exportRequest,
      @RequestParam(value = FORMAT_PARAM, required = false, defaultValue = "json") String format,
      @RequestParam(value = FULL_EXPORT_PARAM, required = false, defaultValue = "true")
      Boolean fullExport, UriComponentsBuilder uriComponentsBuilder) {

    int numberOfNamespacesExportedFrom = exportRequest.getElementUrns().stream()
        .map(e -> e.split(":")[1]).collect(Collectors.toSet()).size();
    if (numberOfNamespacesExportedFrom > 1) {
      return new ResponseEntity<>("Export from more than one namespace is forbidden",
          HttpStatus.BAD_REQUEST);
    }

    MediaType exportMediaType = MediaType.parseMediaType("application/" + format);
    if (!(exportMediaType.equalsTypeAndSubtype(MediaType.APPLICATION_JSON)
        || exportMediaType.equalsTypeAndSubtype(MediaType.APPLICATION_XML))) {
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
    Integer userId = UserHandler.getUserByIdentity(ctx,
        DataElementHubRestApplication.getCurrentUserName()).getId();
    String timestamp = new Timestamp(System.currentTimeMillis())
        .toString().replaceAll("[ \\.\\-\\:]", "_");
    exportService
        .exportService(ctx, exportRequest, userId, exportMediaType,
            fullExport, timestamp, exportService.getExportDirectory());
    UriComponents uriComponents = uriComponentsBuilder.path(
            File.separator + ApiVersion.API_VERSION
                + File.separator + "export"
                + File.separator + "{exportId}")
        .buildAndExpand(timestamp);
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setLocation(uriComponents.toUri());
    return new ResponseEntity<>(httpHeaders, HttpStatus.ACCEPTED);
  }


  /**
   * Returns export file if done otherwise returns export status.
   */
  @GetMapping(produces = {"application/zip",
      MediaType.TEXT_PLAIN_VALUE}, value = "/export/{exportId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity exportStatus(
      @PathVariable(value = "exportId") String exportId,
      @RequestParam(value = "onlyUrns", required = false, defaultValue = "false") Boolean onlyUrns)
      throws Exception {
    Integer userId = UserHandler.getUserByIdentity(ctx,
        DataElementHubRestApplication.getCurrentUserName()).getId();
    ExportInfo exportInfo = exportService
        .exportInfo(exportId, userId, exportService.getExportDirectory());
    StringBuilder stringBuilder = new StringBuilder();
    FileSystemResource fileSystemResource;
    switch (exportInfo.getStatus()) {
      case "NOT DEFINED":
        return new ResponseEntity<>(exportInfo.toString(), HttpStatus.NOT_FOUND);
      case "DONE":
        stringBuilder.append(exportService.getExportDirectory()).append(File.separatorChar);
        stringBuilder.append(userId).append(File.separatorChar);
        stringBuilder.append(exportId).append("-");
        stringBuilder.append(exportInfo.getMediaType().getSubtype()).append("-");
        stringBuilder.append(exportInfo.getStatus().toLowerCase()).append(File.separatorChar);
        if (onlyUrns) {
          stringBuilder.append(EXPORTED_ELEMENTS_FILENAME);
        } else {
          stringBuilder.append(exportId).append(".zip");
        }
        fileSystemResource = new FileSystemResource(stringBuilder.toString());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="
                + fileSystemResource.getFilename())
            .body(fileSystemResource);
      case "EXPIRED":
        if (onlyUrns) {
          stringBuilder.append(exportService.getExportDirectory()).append(File.separatorChar);
          stringBuilder.append(userId).append(File.separatorChar);
          stringBuilder.append(exportId).append("-");
          stringBuilder.append(exportInfo.getMediaType().getSubtype()).append("-");
          stringBuilder.append(exportInfo.getStatus().toLowerCase()).append(File.separatorChar);
          stringBuilder.append(EXPORTED_ELEMENTS_FILENAME);
          fileSystemResource = new FileSystemResource(stringBuilder.toString());
          return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="
                  + fileSystemResource.getFilename())
              .body(fileSystemResource);
        } else {
          return new ResponseEntity<>("This Export has expired on: " + exportInfo
              .getTimestamp().toLocalDateTime().plusDays(exportService.getExpirationPeriodInDays()),
              HttpStatus.NOT_FOUND);
        }
      case "PROCESSING":
        return new ResponseEntity<>(exportInfo.toString(), HttpStatus.ACCEPTED);
      default:
        throw new Exception(String.valueOf(exportService
            .exportInfo(exportId, userId, exportService.getExportDirectory())));
    }
  }


  /**
   * Delete all expired exports.
   */
  @Scheduled(fixedRateString = "${dehub.export.expiredExportsCheckRate}")
  @PostConstruct
  public void deleteExpiredExports() throws IOException {
    Files.createDirectories(Paths.get(exportService.getExportDirectory()));
    final Instant retentionFilePeriod = ZonedDateTime.now()
        .minusDays(exportService.getExpirationPeriodInDays()).toInstant();
    final AtomicInteger countDeletedFiles = new AtomicInteger();
    List<String> parentDirs = new ArrayList<>();
    Files.find(
            Paths.get(exportService.getExportDirectory()),
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
