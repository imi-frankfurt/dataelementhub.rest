package de.dataelementhub.rest.controller.v1;

import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.Deserializer;
import de.dataelementhub.model.MediaType;
import de.dataelementhub.model.dto.ElementRelation;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.ConceptAssociation;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.Slot;
import de.dataelementhub.model.dto.listviews.DataElementGroupMember;
import de.dataelementhub.model.dto.listviews.SimplifiedElementIdentification;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.service.ElementService;
import de.dataelementhub.model.service.JsonValidationService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.jooq.CloseableDSLContext;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Element Controller.
 */
@RestController
@RequestMapping("/" + API_VERSION + "/element")
public class ElementController {

  private ElementService elementService;
  private JsonValidationService jsonValidationService;

  private static final String elementPath = "/" + API_VERSION + "/element/{urn}";

  @Autowired
  public ElementController(ElementService elementService,
      JsonValidationService jsonValidationService) {
    this.elementService = elementService;
    this.jsonValidationService = jsonValidationService;
  }


  /**
   * Create a new Element and return its new ID.
   */
  @PostMapping
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity create(@RequestBody String content,
      UriComponentsBuilder uriComponentsBuilder,
      @RequestHeader(value = HttpHeaders.HOST, required = false) String host,
      @RequestHeader(value = "x-forwarded-proto", required = false) String scheme) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      jsonValidationService.validate(content);
      Element element = Deserializer.getElement(content);
      ScopedIdentifier scopedIdentifier = elementService
          .create(ctx, DataElementHubRestApplication.getCurrentUser().getId(), element);

      UriComponents uriComponents;
      if (host != null && scheme != null) {
        uriComponents =
            uriComponentsBuilder.path("/v1/element/{urn}")
                .host(host)
                .scheme(scheme)
                .buildAndExpand(IdentificationHandler.toUrn(ctx, scopedIdentifier));
      } else {
        uriComponents =
            uriComponentsBuilder.path("/v1/element/{urn}")
                .buildAndExpand(IdentificationHandler.toUrn(ctx, scopedIdentifier));
      }
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (NoSuchElementException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  /**
   * Returns the element with the given URN. If the provided string is not of the used URN format it
   * will be treated as a Namespace name.
   */
  @GetMapping("/{urn}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity read(@PathVariable(value = "urn") String urn,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Element element = elementService
          .read(ctx, DataElementHubRestApplication.getCurrentUser(ctx).getId(), urn);
      if (languages != null) {
        element.applyLanguageFilter(languages);
      }
      return new ResponseEntity<>(element, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Updates an existing Element and return its new URN.
   */
  @PutMapping("/{urn}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity update(@RequestBody String content, @PathVariable("urn") String oldUrn,
      UriComponentsBuilder uriComponentsBuilder,
      @RequestHeader(value = HttpHeaders.HOST, required = false) String host,
      @RequestHeader(value = "x-forwarded-proto", required = false) String scheme) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Identification oldIdentification = IdentificationHandler.fromUrn(ctx, oldUrn);
      if (oldIdentification == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      Element element;
      try {
        element = Deserializer.getElement(content);

        if (oldIdentification.getStatus() == Status.RELEASED && (
            element.getIdentification().getStatus() == Status.STAGED
                || element.getIdentification().getStatus() == Status.DRAFT)) {
          return new ResponseEntity<>("Status change from released to draft or staged not allowed.",
              HttpStatus.BAD_REQUEST);
        }
      } catch (IllegalArgumentException e) {
        // Identification was not set - so it remains unchanged. Reuse the old identifier.
        element = Deserializer.getElement(content, oldIdentification);
      }

      Identification newIdentification = element.getIdentification();
      element.setIdentification(oldIdentification);
      element.getIdentification().setStatus(newIdentification.getStatus());


      // check if namespace status and element status are compatible
      if (ElementHandler.statusMismatch(ctx, DataElementHubRestApplication.getCurrentUser().getId(),
          element)) {
        return new ResponseEntity<>("Unreleased namespaces can't contain released elements",
            HttpStatus.UNPROCESSABLE_ENTITY);
      }
      jsonValidationService.validate(content);
      Identification identification = elementService
          .update(ctx, DataElementHubRestApplication.getCurrentUser().getId(), element);

      UriComponents uriComponents;
      if (host != null && scheme != null) {
        uriComponents =
            uriComponentsBuilder.path("/v1/element/{urn}")
                .host(host)
                .scheme(scheme)
                .buildAndExpand(identification.getUrn());
      } else {
        uriComponents =
            uriComponentsBuilder.path("/v1/element/{urn}")
                .buildAndExpand(identification.getUrn());
      }
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity<>(httpHeaders, HttpStatus.NO_CONTENT);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    } catch (UnsupportedOperationException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    } catch (NoSuchMethodException e) {
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  /**
   * Delete an element. Only drafts are really deleted. Released elements are marked as outdated.
   */
  @DeleteMapping("/{urn}")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity delete(@PathVariable(value = "urn") String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      elementService.delete(ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }


  /**
   * Releases the element with the given URN.
   */
  @PatchMapping("/{urn}/release")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity release(@PathVariable(value = "urn") String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      elementService.release(ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(nse.getMessage(), HttpStatus.NOT_FOUND);
    } catch (IllegalStateException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }


  /**
   * Returns the value domain of the element with the given URN.
   */
  @GetMapping("/{urn}/valuedomain")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getValueDomain(@PathVariable(value = "urn") String urn,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Element element = elementService
          .readValueDomain(ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      if (languages != null) {
        element.applyLanguageFilter(languages);
      }
      return new ResponseEntity<>(element, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Returns the definitions of the element with the given URN.
   */
  @GetMapping("/{urn}/definitions")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getDefinitions(@PathVariable(value = "urn") String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      List<Definition> definitions = elementService
          .readDefinitions(ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      return new ResponseEntity<>(definitions, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Returns the slots of the element with the given URN.
   */
  @GetMapping("/{urn}/slots")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getSlots(@PathVariable(value = "urn") String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      List<Slot> slots = elementService
          .readSlots(ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      return new ResponseEntity<>(slots, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Returns the identification of the element with the given URN.
   */
  @GetMapping("/{urn}/identification")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getIdentification(@PathVariable(value = "urn") String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Identification identification = elementService
          .readIdentification(ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      return new ResponseEntity<>(identification, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Returns the Concept Associations of the element with the given URN.
   */
  @GetMapping("/{urn}/concepts")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getConceptAssociations(@PathVariable(value = "urn") String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      List<ConceptAssociation> conceptAssociations = elementService
          .readConceptAssociations(
              ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      return new ResponseEntity<>(conceptAssociations, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }


  /**
   * Returns the relations of the element with the given URN.
   */
  @GetMapping("/{urn}/relations")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getRelations(@PathVariable(value = "urn") String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      List<ElementRelation> relations = elementService
          .readRelations(ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      return new ResponseEntity<>(relations, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }


  /**
   * Get DataElementGroup or Record Members as a List using its urn.
   */
  @GetMapping("/{urn}/members")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getMembers(
      @PathVariable(value = "urn") String urn,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String responseType) {
    try {
      try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
        List<Member> members =
            elementService.readMembers(
                ctx, DataElementHubRestApplication.getCurrentUser(ctx).getId(), urn);

        if (responseType != null && responseType
            .equalsIgnoreCase(MediaType.JSON_LIST_VIEW.getLiteral())) {
          List<DataElementGroupMember> dataElementGroupMembers = new ArrayList<>();
          members.forEach(m -> {
            Element element = elementService.read(
                ctx, DataElementHubRestApplication.getCurrentUser().getId(), m.getElementUrn());
            element.applyLanguageFilter(languages);
            dataElementGroupMembers.add(new DataElementGroupMember(element));
          });
          return new ResponseEntity<>(dataElementGroupMembers, HttpStatus.OK);
        } else {
          return new ResponseEntity<>(members, HttpStatus.OK);
        }
      }
    } catch (NoSuchElementException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Update dataElementGroup or record members.
   * If at least one member has new version return the new element location
   * otherwise return the old one.
   */
  @PostMapping("/{urn}/updateMembers")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity updateMembers(@PathVariable(value = "urn") String urn,
      UriComponentsBuilder uriComponentsBuilder) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      String newUrn = elementService.updateMembers(
          ctx, DataElementHubRestApplication.getCurrentUser().getId(), urn);
      UriComponents uriComponents;
      uriComponents =
          uriComponentsBuilder.path(elementPath).buildAndExpand(newUrn);
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
    } catch (NoSuchElementException nse) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (IllegalAccessException e) {
      return new ResponseEntity<>(e, HttpStatus.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Get all available paths for a given element.
   */
  @GetMapping("/{urn}/paths")
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity getElementPaths(@PathVariable(value = "urn") String urn,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String languages) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      int userId = DataElementHubRestApplication.getCurrentUser().getId();
      List<List<SimplifiedElementIdentification>> elementPaths =
          elementService.getElementPaths(ctx, userId, urn, languages);
      return new ResponseEntity<>(elementPaths, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e, HttpStatus.BAD_REQUEST);
    } catch (IllegalStateException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }
}
