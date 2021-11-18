package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.jooq.enums.GrantType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.Deserializer;
import de.dataelementhub.model.MediaType;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.listviews.NamespaceMember;
import de.dataelementhub.model.service.ElementService;
import de.dataelementhub.model.service.JsonValidationService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/v1/namespaces")
public class NamespaceController {

  private ElementService elementService;
  private JsonValidationService jsonValidationService;

  @Autowired
  public NamespaceController(ElementService elementService,
      JsonValidationService jsonValidationService) {
    this.elementService = elementService;
    this.jsonValidationService = jsonValidationService;
  }

  /**
   * TODO.
   */
  @GetMapping("/writable")
  public List<Namespace> getWritable() {
    /*try (DSLContext ctx = ResourceManager.getDslContext()) {
      DehubUser user = DataElementHubRestApplication.getCurrentUser();
      return NamespaceHandler.getWritable(ctx, user.getId());
    }*/
    return new ArrayList<>(); // TODO remove
  }

  /**
   * Get a list of all namespaces a user has any access to. Can be limited to a certain access type
   * by setting the "scope" parameter to either 'READ', 'WRITE' or 'ADMIN'.
   */
  @GetMapping("")
  public ResponseEntity getNamespaces(
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String responseType,
      @RequestParam(name = "scope", required = false) String scope) {

    Map<GrantType, List<Namespace>> namespaceMap;

    if (scope == null) {
      namespaceMap = elementService
          .readNamespaces(DataElementHubRestApplication.getCurrentUser().getId());
    } else {
      try {
        GrantType scopeGrantType = GrantType.valueOf(scope.toUpperCase());
        namespaceMap = elementService
            .readNamespaces(DataElementHubRestApplication.getCurrentUser().getId(), scopeGrantType);
      } catch (IllegalAccessException e) {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
      } catch (IllegalArgumentException e) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
    }
    if (languages != null) {
      namespaceMap.entrySet().stream()
          .forEach(es -> es.getValue().forEach(ns -> ns.applyLanguageFilter(languages)));
    }
    if (responseType != null && responseType
        .equalsIgnoreCase(MediaType.JSON_LIST_VIEW.getLiteral())) {
      Map<GrantType, List<de.dataelementhub.model.dto.listviews.Namespace>> namespaceListViewMap
          = new HashMap<>();
      namespaceMap.entrySet().stream()
          .forEach(es -> {
            List<de.dataelementhub.model.dto.listviews.Namespace> nsviews = new ArrayList<>();
            es.getValue()
                .forEach(ns ->
                    nsviews.add(new de.dataelementhub.model.dto.listviews.Namespace(ns)));
            namespaceListViewMap.put(es.getKey(), nsviews);
          });
      return new ResponseEntity(namespaceListViewMap, HttpStatus.OK);
    } else {
      return new ResponseEntity(namespaceMap, HttpStatus.OK);
    }
  }

  /**
   * Create a new Namespace and return its new ID.
   */
  @PostMapping
  public ResponseEntity create(@RequestBody String content,
      UriComponentsBuilder uriComponentsBuilder,
      @RequestHeader(value = HttpHeaders.HOST, required = false) String host,
      @RequestHeader(value = "x-forwarded-proto", required = false) String scheme) {
    try {
      jsonValidationService.validate(content);
      Element element = Deserializer.getElement(content);
      Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
      ScopedIdentifier scopedIdentifier = elementService.create(userId, element);

      UriComponents uriComponents;
      if (host != null && scheme != null) {
        uriComponents =
            uriComponentsBuilder.path("/v1/namespaces/{id}")
                .host(host)
                .scheme(scheme)
                .buildAndExpand(scopedIdentifier.getIdentifier());
      } else {
        uriComponents =
            uriComponentsBuilder.path("/v1/namespaces/{id}")
                .buildAndExpand(scopedIdentifier.getIdentifier());
      }
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (IOException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  /**
   * Read a namespace by its id.
   */
  @GetMapping("/{namespaceId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity read(@PathVariable(value = "namespaceId") String namespaceId,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages) {
    try {
      Element element = elementService.read(DataElementHubRestApplication.getCurrentUser().getId(), namespaceId);
      if (languages != null) {
        element.applyLanguageFilter(languages);
      }
      return new ResponseEntity<>(element, HttpStatus.OK);
    } catch (NoSuchElementException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Get namespace Members as a List using namespaceIdentifier.
   */
  @GetMapping("/{namespaceId}/members")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity readNamespaceMembers(
      @PathVariable(value = "namespaceId") Integer namespaceId,
      @RequestParam(value = "elementType", required = false) List<String> elementTypes,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String responseType) {
    try {
      if (responseType != null && responseType
          .equalsIgnoreCase(MediaType.JSON_LIST_VIEW.getLiteral())) {

        List<NamespaceMember> namespaceMembers = elementService.getNamespaceMembersListview(
            DataElementHubRestApplication.getCurrentUser().getId(), namespaceId, elementTypes);

        namespaceMembers.forEach(nsm -> {
          nsm.applyLanguageFilter(languages);
        });

        return new ResponseEntity<>(namespaceMembers, HttpStatus.OK);
      } else {
        List<Member> namespaceMembers =
            elementService.readNamespaceMembers(
                DataElementHubRestApplication.getCurrentUser().getId(), namespaceId, elementTypes);
        return new ResponseEntity<>(namespaceMembers, HttpStatus.OK);
      }
    } catch (NoSuchElementException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Updates an existing Namespace.
   */
  @PutMapping("/{namespaceId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity update(@PathVariable(value = "namespaceId") String oldNamespaceId,
      @RequestBody String content, UriComponentsBuilder uriComponentsBuilder,
      @RequestHeader(value = HttpHeaders.HOST, required = false) String host,
      @RequestHeader(value = "x-forwarded-proto", required = false) String scheme) {
    Element element = Deserializer.getElement(content);
    Integer userId = DataElementHubRestApplication.getCurrentUser().getId();
    Element oldNamespace = elementService
        .read(DataElementHubRestApplication.getCurrentUser().getId(), oldNamespaceId);

    if (oldNamespace == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    if (oldNamespace.getIdentification().getStatus() == Status.RELEASED && (
        element.getIdentification().getStatus() == Status.STAGED
            || element.getIdentification().getStatus() == Status.DRAFT)) {
      return new ResponseEntity<>("Status change from released to draft or staged not allowed.",
          HttpStatus.BAD_REQUEST);
    }

    Identification newIdentification = element.getIdentification();
    element.setIdentification(oldNamespace.getIdentification());
    element.getIdentification().setHideNamespace(newIdentification.getHideNamespace());
    element.getIdentification().setStatus(newIdentification.getStatus());

    try {
      jsonValidationService.validate(content);
      Identification identification = elementService.update(userId, element);

      UriComponents uriComponents;
      if (host != null && scheme != null) {
        uriComponents =
            uriComponentsBuilder.path("/v1/namespaces/{id}")
                .host(host)
                .scheme(scheme)
                .buildAndExpand(identification.getIdentifier());
      } else {
        uriComponents =
            uriComponentsBuilder.path("/v1/namespaces/{id}")
                .buildAndExpand(identification.getIdentifier());
      }
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity<>(httpHeaders, HttpStatus.NO_CONTENT);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (UnsupportedOperationException | IllegalArgumentException e) {
      return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
    } catch (NoSuchMethodException e) {
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    } catch (IOException | IllegalStateException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  /**
   * Delete a namespace. Only drafts are really deleted. Released namespaces are marked as
   * outdated.
   */
  @DeleteMapping("/{namespaceId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity delete(@PathVariable(value = "namespaceId") String namespaceId) {
    try {
      Element namespace = elementService
          .read(DataElementHubRestApplication.getCurrentUser().getId(), namespaceId);
      elementService.delete(DataElementHubRestApplication.getCurrentUser().getId(),
          namespace.getIdentification().getUrn());
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (IllegalStateException e) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
  }

  /**
   * Release a namespace.
   */
  @PatchMapping("/{namespaceId}/release")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity release(@PathVariable(value = "namespaceId") String namespaceId) {
    try {
      Element namespace = elementService
          .read(DataElementHubRestApplication.getCurrentUser().getId(), namespaceId);
      elementService.release(DataElementHubRestApplication.getCurrentUser().getId(),
          namespace.getIdentification().getUrn());
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(nse.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

}
