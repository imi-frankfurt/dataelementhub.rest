package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.search.SearchRequest;
import de.dataelementhub.model.handler.UserHandler;
import de.dataelementhub.model.service.SearchService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search Controller.
 */
@Transactional
@RestController
@RequestMapping("/v1/search")
public class SearchController {

  private final SearchService searchService;

  private final DSLContext ctx;

  @Autowired
  public SearchController(SearchService searchService, DSLContext ctx) {
    this.searchService = searchService;
    this.ctx = ctx;
  }

  /**
   * Returns search results matching all specifications in searchRequest as a list of DataElementHub
   * Elements. If no results found an empty list will be returned.
   */
  @GetMapping
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity<List<? extends Object>> search(
      @RequestParam(value = "query", required = true) String searchText,
      @RequestParam(value = "type", required = false) List<String> typesString,
      @RequestParam(value = "status", required = false) List<String> statusString,
      @RequestParam(value = "section", required = false) List<String> elementPartsString
  ) {
    List<ElementType> types = new ArrayList<>();
    List<Status> status = new ArrayList<>();
    List<String> availableElementPartsString = Arrays.asList("definition", "designation", "slotkey",
        "slotvalue", "conceptassociationsystem", "conceptassociationterm", "conceptassociationtext",
        "datatype", "valuedomaindescription", "unitofmeasure", "format");
    try {
      if (typesString != null) {
        for (String ts : typesString) {
          types.add(ElementType.valueOf(ts));
        }
      } else {
        types = Arrays.asList(ElementType.values());
      }


      if (statusString != null) {
        for (String s : statusString) {
          status.add(Status.valueOf(s));
        }
      } else {
        status = Arrays.asList(Status.values());
      }

      // Insert default values if no restriction was set via query param
      if (elementPartsString == null) {
        elementPartsString = availableElementPartsString;
      } else {
        elementPartsString = elementPartsString.stream().map(String::toLowerCase).collect(
            Collectors.toList());
        if (!availableElementPartsString.containsAll(elementPartsString)) {
          throw new IllegalArgumentException("Element section is not valid!");
        }
      }
    } catch (IllegalArgumentException iae) {
      return new ResponseEntity<>(
          Collections.singletonList(iae.getMessage()), HttpStatus.UNPROCESSABLE_ENTITY);
    }
    int userId = UserHandler.getUserByIdentity(ctx,
        DataElementHubRestApplication.getCurrentUserName()).getId();
    SearchRequest searchRequest = new SearchRequest(searchText, types, status, elementPartsString);

    List<Element> elements = searchService.search(
        ctx, searchRequest, userId);
    return new ResponseEntity<>(elements, HttpStatus.OK);

  }
}
