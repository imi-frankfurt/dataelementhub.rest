package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.importexport.ImportInfo;
import de.dataelementhub.model.dto.listviews.StagedElement;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.service.ImportService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.PostConstruct;
import org.jooq.CloseableDSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/**
 * Import Controller.
 */
@RestController
@RequestMapping("/v1/import")
public class ImportController {

  public String importDirectory = System.getProperty("java.io.tmpdir") + "/uploads"
      .replace('/', File.separatorChar);

  private final ImportService importService;

  @Autowired
  public ImportController(ImportService importService) {
    this.importService = importService;
  }

  /**
   * Get an overview about all user imports and their status.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @GetMapping(value = "")
  public ResponseEntity<List<ImportInfo>> listAllImports() {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
      List<ImportInfo> importInfoList = importService.listAllImports(ctx, userId);
      return new ResponseEntity<>(importInfoList, HttpStatus.OK);
    }

  }

  /**
   * Receive import files, start import process and return import id.
   */
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE},
      value = "")
  public ResponseEntity<String> importFiles(@RequestBody List<MultipartFile> files,
      @RequestParam("namespaceUrn") String namespaceUrn,
      UriComponentsBuilder uriComponentsBuilder) {
    Integer namespaceIdentifier = IdentificationHandler.getIdentifierFromUrn(namespaceUrn);
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (!DaoUtil.accessLevelGranted(
          ctx, namespaceIdentifier, userId, DaoUtil.WRITE_ACCESS_TYPES)) {
        return new ResponseEntity<>(
            "Only users with WRITE or ADMIN accessLevel can import to this namespace.",
            HttpStatus.UNAUTHORIZED);
      }
      if (files == null) {
        return new ResponseEntity<>(
            "No file was submitted.", HttpStatus.BAD_REQUEST);
      }
      int importId = importService.generateImportId(ctx, namespaceUrn, userId, files,
          importDirectory);
      UriComponents uriComponents = uriComponentsBuilder.path("/v1/import/{importId}")
          .buildAndExpand(importId);
      if (importId > -1) {
        importService.execute(ctx, files, importDirectory, userId, importId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(uriComponents.toUri());
        return new ResponseEntity(httpHeaders, HttpStatus.ACCEPTED);
      } else {
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
      }
    } catch (IOException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  /**
   * Get import info by ID.
   */
  @GetMapping(value = "/{importId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity<String> importInfo(
      @PathVariable(value = "importId") String importId) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
      ImportInfo importInfo = importService.getImportInfo(ctx, Integer.parseInt(importId), userId);
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
  @GetMapping(value = "/{importId}/members")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getStagedElementsListView(
      @RequestParam(value = "hideSubElements", required = false, defaultValue = "false")
          Boolean hideSubElements,
      @RequestParam(value = "onlyConverted", required = false, defaultValue = "false")
          Boolean onlyConverted,
      @PathVariable(value = "importId") Integer importId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<StagedElement> stagedElements = new ArrayList<>();
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      stagedElements = importService.getImportMembersListView(
          ctx, importId, userId, hideSubElements, onlyConverted);
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
  @GetMapping(value = "/{importId}/{stagedElementId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getStagedElement(
      @PathVariable(value = "importId") Integer importId,
      @PathVariable(value = "stagedElementId") String stagedElementId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    de.dataelementhub.model.dto.element.StagedElement stagedElement;
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      stagedElement = importService.getStagedElement(ctx, importId, userId, stagedElementId);
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
  @GetMapping(value = "/{importId}/{stagedElementId}/members")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getStagedElementMembers(
      @RequestParam(value = "onlyConverted", required = false, defaultValue = "false")
          Boolean onlyConverted,
      @PathVariable(value = "importId") Integer importId,
      @PathVariable(value = "stagedElementId") String stagedElementId) {
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    List<StagedElement> stagedElements;
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      stagedElements = importService.getStagedElementMembers(
          ctx, importId, userId, stagedElementId, onlyConverted);
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
  @PostMapping(value = "/{importId}/convert")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity convertStagedElementsToDrafts(
      @PathVariable(value = "importId") Integer importId,
      @RequestBody List<String> stagedElementsUrns) throws Exception {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
      importService.convertToDraft(ctx, stagedElementsUrns, userId, importId);
      return new ResponseEntity(HttpStatus.OK);
    }
  }

  /**
   * Delete stagedImport by Id.
   */
  @DeleteMapping(value = "/{importId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity deleteStagedImport(
      @PathVariable(value = "importId") Integer importId) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
      importService.deleteStagedImport(ctx, userId, importId);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalAccessException ie) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException ne) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Create the importDirectory after the initialization of bean properties.
   */
  @PostConstruct
  public void createImportDirectory() throws IOException {
    Files.createDirectories(Paths.get(importDirectory));
  }
}
