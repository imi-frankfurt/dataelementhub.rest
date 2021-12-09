package de.dataelementhub.rest.controller.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.dataelementhub.dal.jooq.enums.GrantType;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.importdto.ImportInfo;
import de.dataelementhub.model.dto.listviews.StagedElement;
import de.dataelementhub.model.service.ImportService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class ImportController {

  public String importDirectory = System.getProperty("java.io.tmpdir") + "/uploads";

  private ImportService importService;

  @Autowired
  public void ImportExportController(ImportService importService) {
    this.importService = importService;
  }

  /**
   * Get an overview about all user imports and their status.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "/import")
  public ResponseEntity<List<ImportInfo>> allImports() {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<ImportInfo> importDescriptions = importService.allImports(userId);
    return new ResponseEntity<List<ImportInfo>>(importDescriptions, HttpStatus.OK);
  }

  /**
   * Receive import files, start import process and return import id.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(consumes = {"multipart/form-data", "application/json"}, value = "/import")
  public ResponseEntity<String> importFiles(@RequestBody List<MultipartFile> file,
      @RequestParam("namespaceUrn") String namespaceUrn) {
    Integer namespaceIdentifier = Integer.valueOf(namespaceUrn.split(":")[1]);
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    String timestamp = new Timestamp(System.currentTimeMillis())
        .toString().replaceAll("[ \\.\\-\\:]", "_");
    if (!DaoUtil.checkGrants(namespaceIdentifier, userId,
        Arrays.asList(GrantType.ADMIN, GrantType.WRITE))) {
      return new ResponseEntity<>(
          "Only users with WRITE or ADMIN Grant can import to this namespace.",
          HttpStatus.UNAUTHORIZED);
    }
    int importId = importService
        .generateImportId(namespaceUrn, userId, file, importDirectory, timestamp);
    if (importId > -1) {
      importService.importService(file, importDirectory, userId, importId, timestamp);
      return new ResponseEntity(importId, HttpStatus.ACCEPTED);
    } else {
      return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Check import status and return it.
   */
  @GetMapping(value = "/import/{importId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity<String> importStatus(
      @PathVariable(value = "importId") String importId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    switch (importService.checkStatus(importId, userId)) {
      case DONE:
        return new ResponseEntity<>("IMPORT COMPLETED", HttpStatus.OK);
      case PROCESSING:
        return new ResponseEntity<>("PROCESSING", HttpStatus.ACCEPTED);
      case INTERRUPTED:
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      default:
        return new ResponseEntity<>("Import ID: "
            + importId + " is not defined!", HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Return imported stagedElements in listView format by importId.
   */
  @GetMapping(value = "/import/{importId}/members")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getStagedElementsListView(
      @RequestParam(value = "hideSubElements", required = false, defaultValue = "false")
          Boolean hideSubElements,
      @PathVariable(value = "importId") Integer importId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<StagedElement> stagedElements =
        importService.getImportMembersListView(importId, userId, hideSubElements);
    return new ResponseEntity(stagedElements, HttpStatus.OK);
  }

  /**
   * Get stagedElement by Id.
   */
  @GetMapping(value = "/import/{importId}/{stagedElementId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getStagedElement(
      @PathVariable(value = "importId") Integer importId,
      @PathVariable(value = "stagedElementId") String stagedElementId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    de.dataelementhub.model.dto.element.StagedElement stagedElement =
        null;
    try {
      stagedElement = importService.getStagedElement(importId, userId, stagedElementId);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    if (stagedElement == null) {
      return new ResponseEntity(stagedElement, HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity(stagedElement, HttpStatus.OK);
  }

  /**
   * Get stagedElementMembers by Id.
   */
  @GetMapping(value = "/import/{importId}/{stagedElementId}/members")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getStagedElementMembers(
      @PathVariable(value = "importId") Integer importId,
      @PathVariable(value = "stagedElementId") String stagedElementId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<StagedElement> stagedElements =
        importService.getStagedElementMembers(importId, userId, stagedElementId);
    return new ResponseEntity(stagedElements, HttpStatus.OK);
  }

  @PostConstruct
  public void createImportDirectory() throws IOException {
    Files.createDirectories(Paths.get(importDirectory));
  }
}
