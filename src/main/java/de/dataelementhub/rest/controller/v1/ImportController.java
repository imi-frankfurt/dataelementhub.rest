package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.importdto.ImportInfo;
import de.dataelementhub.model.dto.listviews.StagedElement;
import de.dataelementhub.model.service.ImportService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/v1")
public class ImportController {

  public String importDirectory = System.getProperty("java.io.tmpdir") + "/uploads";

  private final ImportService importService;

  @Autowired
  public ImportController(ImportService importService) {
    this.importService = importService;
  }

  /**
   * Get an overview about all user imports and their status.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "/import")
  public ResponseEntity<List<ImportInfo>> allImports() {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<ImportInfo> importInfoList = importService.allImports(userId);
    return new ResponseEntity<List<ImportInfo>>(importInfoList, HttpStatus.OK);
  }

  /**
   * Receive import files, start import process and return import id.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(consumes = {"multipart/form-data", "application/json"}, value = "/import")
  public ResponseEntity<String> importFiles(@RequestBody List<MultipartFile> file,
      @RequestParam("namespaceUrn") String namespaceUrn,
      UriComponentsBuilder uriComponentsBuilder) {
    Integer namespaceIdentifier = Integer.valueOf(namespaceUrn.split(":")[1]);
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    String timestamp = new Timestamp(System.currentTimeMillis())
        .toString().replaceAll("[ \\.\\-\\:]", "_");
    if (!DaoUtil.checkGrants(namespaceIdentifier, userId,
        Arrays.asList(AccessLevelType.ADMIN, AccessLevelType.WRITE))) {
      return new ResponseEntity<>(
          "Only users with WRITE or ADMIN Grant can import to this namespace.",
          HttpStatus.UNAUTHORIZED);
    }
    if (file == null) {
      return new ResponseEntity<>(
          "Uploaded File is not Valid.",
          HttpStatus.BAD_REQUEST);
    }
    int importId = 0;
    try {
      importId = importService
          .generateImportId(namespaceUrn, userId, file, importDirectory, timestamp);
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
    UriComponents uriComponents =
        uriComponentsBuilder.path("/v1/import/{importId}")
            .buildAndExpand(importId);
    if (importId > -1) {
      importService.importService(file, importDirectory, userId, importId, timestamp);
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity(httpHeaders, HttpStatus.ACCEPTED);
    } else {
      return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Get import info by ID.
   */
  @GetMapping(value = "/import/{importId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity<String> importInfo(
      @PathVariable(value = "importId") String importId) {
    try {
      Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
      ImportInfo importInfo = importService.getImportInfo(Integer.parseInt(importId), userId);
      return new ResponseEntity(importInfo, HttpStatus.OK);
    } catch (NoSuchElementException ex) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
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
      @RequestParam(value = "onlyConverted", required = false, defaultValue = "false")
          Boolean onlyConverted,
      @PathVariable(value = "importId") Integer importId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<StagedElement> stagedElements = new ArrayList<>();
    try {
      stagedElements = importService.getImportMembersListView(importId, userId, hideSubElements,
          onlyConverted);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException ex) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
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
    de.dataelementhub.model.dto.element.StagedElement stagedElement = null;
    try {
      stagedElement = importService.getStagedElement(importId, userId, stagedElementId);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
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
      @RequestParam(value = "onlyConverted", required = false, defaultValue = "false")
          Boolean onlyConverted,
      @PathVariable(value = "importId") Integer importId,
      @PathVariable(value = "stagedElementId") String stagedElementId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<StagedElement> stagedElements =
        null;
    try {
      stagedElements = importService.getStagedElementMembers(
          importId, userId, stagedElementId, onlyConverted);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException ex) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity(stagedElements, HttpStatus.OK);
  }

  /**
   * Convert stagedElements to Drafts.
   */
  @PostMapping(value = "/import/{importId}/convert")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity convertStagedElementsToDrafts(
      @PathVariable(value = "importId") Integer importId,
      @RequestBody List<String> stagedElementsUrns) throws Exception {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    importService.convertToDraft(stagedElementsUrns, userId, importId);
    return new ResponseEntity(HttpStatus.OK);
  }

  /**
   * Check user grants then delete staged import.
   */
  @DeleteMapping(value = "/import/{importId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity deleteStagedImport(
      @PathVariable(value = "importId") Integer importId) {
    try {
      Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
      importService.deleteStagedImport(userId, importId);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalAccessException ie) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException ne) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @PostConstruct
  public void createImportDirectory() throws IOException {
    Files.createDirectories(Paths.get(importDirectory));
  }
}
