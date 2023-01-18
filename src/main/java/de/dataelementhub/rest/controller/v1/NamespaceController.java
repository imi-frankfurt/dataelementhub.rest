package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.Deserializer;
import de.dataelementhub.model.MediaType;
import de.dataelementhub.model.dto.DeHubUserPermission;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.listviews.NamespaceMember;
import de.dataelementhub.model.handler.UserHandler;
import de.dataelementhub.model.service.JsonValidationService;
import de.dataelementhub.model.service.NamespaceService;
import de.dataelementhub.model.service.UserService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Namespace Controller.
 */
@Transactional
@RestController
@RequestMapping("/v1/namespaces")
public class NamespaceController {

  private NamespaceService namespaceService;
  private UserService userService;
  private JsonValidationService jsonValidationService;

  private final DSLContext ctx;

  /**
   * Create a new NamespaceController.
   */
  @Autowired
  public NamespaceController(NamespaceService namespaceService, UserService userService,
      JsonValidationService jsonValidationService, DSLContext ctx) {
    this.namespaceService = namespaceService;
    this.userService = userService;
    this.jsonValidationService = jsonValidationService;
    this.ctx = ctx;
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

    Map<AccessLevelType, List<Namespace>> namespaceMap;
    DehubUser dehubUser = UserHandler.getUserByIdentity(ctx,
        DataElementHubRestApplication.getCurrentUserName());
    if (scope == null) {
      namespaceMap = namespaceService.readNamespaces(ctx, dehubUser.getId());
    } else {
      try {
        AccessLevelType scopeAccessLevel = AccessLevelType.valueOf(scope.toUpperCase());
        namespaceMap = namespaceService.readNamespaces(ctx, dehubUser.getId(), scopeAccessLevel);
      } catch (IllegalAccessException e) {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
      } catch (IllegalArgumentException e) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
    }
    if (languages != null) {
      namespaceMap.entrySet()
          .forEach(es -> es.getValue().forEach(ns -> ns.applyLanguageFilter(languages)));
    }
    if (responseType != null && responseType
        .equalsIgnoreCase(MediaType.JSON_LIST_VIEW.getLiteral())) {
      Map<AccessLevelType, List<de.dataelementhub.model.dto.listviews.Namespace>>
          namespaceListViewMap = new HashMap<>();
      namespaceMap.entrySet()
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
  @PreAuthorize("hasRole('ROLE_createNamespace')")
  public ResponseEntity create(@RequestBody String content,
      UriComponentsBuilder uriComponentsBuilder,
      @RequestHeader(value = HttpHeaders.HOST, required = false) String host,
      @RequestHeader(value = "x-forwarded-proto", required = false) String scheme) {
    try  {
      jsonValidationService.validate(content);
      Element element = Deserializer.getElement(content);
      Integer userId = UserHandler.getUserByIdentity(ctx,
          DataElementHubRestApplication.getCurrentUserName()).getId();
      ScopedIdentifier scopedIdentifier = namespaceService.create(ctx, userId, element);

      UriComponents uriComponents;
      if (host != null && scheme != null) {
        uriComponents =
            uriComponentsBuilder
                .path("/v1/namespaces/{id}")
                .host(host)
                .scheme(scheme)
                .buildAndExpand(scopedIdentifier.getIdentifier());
      } else {
        uriComponents =
            uriComponentsBuilder
                .path("/v1/namespaces/{id}")
                .buildAndExpand(scopedIdentifier.getIdentifier());
      }
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (IOException | IllegalArgumentException e) {
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
    try  {
      Element element = namespaceService.read(ctx,
          UserHandler.getUserByIdentity(ctx,
              DataElementHubRestApplication.getCurrentUserName()).getId(), namespaceId);
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
      @RequestParam(value = "hideSubElements", required = false) Boolean hideSubElements,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String responseType) {
    try  {
      if (hideSubElements == null) {
        hideSubElements = false;
      }
      DehubUser dehubUser = UserHandler.getUserByIdentity(ctx,
          DataElementHubRestApplication.getCurrentUserName());
      if (responseType != null && responseType
          .equalsIgnoreCase(MediaType.JSON_LIST_VIEW.getLiteral())) {

        List<NamespaceMember> namespaceMembers = namespaceService.getNamespaceMembersListview(ctx,
            dehubUser.getId(), namespaceId, elementTypes, hideSubElements);

        namespaceMembers.forEach(nsm -> nsm.applyLanguageFilter(languages));

        return new ResponseEntity<>(namespaceMembers, HttpStatus.OK);
      } else {
        List<Member> namespaceMembers =
            namespaceService.readNamespaceMembers(
                ctx,
                dehubUser.getId(),
                namespaceId,
                elementTypes,
                hideSubElements);
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
    Integer userId = UserHandler.getUserByIdentity(ctx,
        DataElementHubRestApplication.getCurrentUserName()).getId();
    try  {
      Element oldNamespace = namespaceService.read(ctx, userId, oldNamespaceId);

      if (oldNamespace.getIdentification().getStatus() == Status.RELEASED
          && element.getIdentification().getStatus() == Status.DRAFT) {
        return new ResponseEntity<>("Status change from released to draft or staged not allowed.",
            HttpStatus.BAD_REQUEST);
      }

      Identification newIdentification = element.getIdentification();
      element.setIdentification(oldNamespace.getIdentification());
      element.getIdentification().setHideNamespace(newIdentification.getHideNamespace());
      element.getIdentification().setStatus(newIdentification.getStatus());

      jsonValidationService.validate(content);
      Identification identification = namespaceService.update(ctx, userId, element);

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
    } catch (UnsupportedOperationException e) {
      return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
    } catch (NoSuchMethodException e) {
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    } catch (IOException | IllegalArgumentException | IllegalStateException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(
          "NamespaceId: " + oldNamespaceId + " does not exist!", HttpStatus.NOT_FOUND);
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
      DehubUser dehubUser = UserHandler.getUserByIdentity(ctx,
          DataElementHubRestApplication.getCurrentUserName());
      Element namespace = namespaceService.read(ctx, dehubUser.getId(), namespaceId);
      namespaceService.delete(ctx, dehubUser.getId(), namespace.getIdentification().getUrn());
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
      DehubUser dehubUser = UserHandler.getUserByIdentity(ctx,
          DataElementHubRestApplication.getCurrentUserName());
      Element namespace = namespaceService.read(ctx, dehubUser.getId(), namespaceId);
      namespaceService.release(ctx, dehubUser.getId(), namespace.getIdentification().getUrn());
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(nse.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Read user access to namespace by namespace identifier.
   */
  @GetMapping("/{namespaceIdentifier}/access")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity readAccessLevels(
      @PathVariable(value = "namespaceIdentifier") String namespaceIdentifier) {
    DehubUser dehubUser = UserHandler.getUserByIdentity(ctx,
        DataElementHubRestApplication.getCurrentUserName());

    if (dehubUser.getId() < 0) {
      return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }
    try  {
      namespaceService.read(ctx, dehubUser.getId(), namespaceIdentifier);
      List<DeHubUserPermission> userPermissions = namespaceService.readUserAccessList(ctx,
          dehubUser.getId(), Integer.parseInt(namespaceIdentifier));

      return new ResponseEntity(userPermissions, HttpStatus.OK);
    } catch (IllegalAccessException e) {
      return new ResponseEntity(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Update user access to namespace.
   */
  @PatchMapping("/{namespaceIdentifier}/access")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity changeAccessLevels(
      @PathVariable(value = "namespaceIdentifier") String namespaceIdentifier,
      @RequestBody List<DeHubUserPermission> permissions) {

    try {
      DehubUser dehubUser = UserHandler.getUserByIdentity(ctx,
          DataElementHubRestApplication.getCurrentUserName());
      namespaceService.read(ctx, dehubUser.getId(), namespaceIdentifier);
      userService.grantAccessToNamespace(ctx, dehubUser.getId(),
          Integer.parseInt(namespaceIdentifier), permissions);
      return new ResponseEntity(HttpStatus.NO_CONTENT);
    } catch (IllegalAccessException e) {
      return new ResponseEntity(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Remove user access from namespace.
   */
  @DeleteMapping("/{namespaceIdentifier}/access/{userAuthId}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity deleteAccessLevels(
      @PathVariable(value = "namespaceIdentifier") String namespaceIdentifier,
      @PathVariable(value = "userAuthId") String userAuthId) {

    try {
      DehubUser dehubUser = UserHandler.getUserByIdentity(ctx,
          DataElementHubRestApplication.getCurrentUserName());
      namespaceService.read(ctx, dehubUser.getId(), namespaceIdentifier);
      userService.revokeAccessToNamespace(ctx, dehubUser.getId(),
          Integer.parseInt(namespaceIdentifier), userAuthId);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

}
