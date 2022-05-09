package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.search.SearchRequest;
import de.dataelementhub.model.service.SearchService;
import de.dataelementhub.rest.DataElementHubRestApplication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search Controller.
 */
@RestController
@RequestMapping("/v1/search")
public class SearchController {

  private final SearchService searchService;

  @Autowired
  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * Returns search results matching all specifications in searchRequest as a list of DataElementHub
   * Elements. If no results found an empty list will be returned.
   */
  @GetMapping
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity<List<Element>> search(
      @RequestParam(value = "query", required = true) String searchText,
      @RequestParam(value = "type", required = false) List<String> typesString,
      @RequestParam(value = "status", required = false) List<String> statusString,
      @RequestParam(value = "section", required = false) List<String> elementPartsString
  ) {
    List<ElementType> types = new ArrayList<>();
    if (typesString != null) {
      for (String ts : typesString) {
        types.add(ElementType.valueOf(ts));
      }
    } else {
      types = Arrays.asList(ElementType.values());
    }

    List<Status> status = new ArrayList<>();
    if (statusString != null) {
      for (String s : statusString) {
        status.add(Status.valueOf(s));
      }
    } else {
      status = Arrays.asList(Status.values());
    }

    // Insert default values if no restriction was set via query param
    if (elementPartsString == null) {
      elementPartsString = Arrays.asList("definition", "designation", "slotKey", "slotValue",
          "conceptAssociationSystem", "conceptAssociationTerm", "conceptAssociationText",
          "dataType", "valueDomainDescription", "unitOfMeasure", "format");
    }

    int userId = DataElementHubRestApplication.getCurrentUser().getId();
    SearchRequest searchRequest = new SearchRequest(searchText, types, status, elementPartsString);
    List<Element> elements = searchService.search(
        searchRequest, userId);
    return new ResponseEntity<>(elements, HttpStatus.OK);
  }
}
