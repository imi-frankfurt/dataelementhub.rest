package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.jooq.enums.SourceType;
import de.dataelementhub.dal.jooq.tables.pojos.Source;
import de.dataelementhub.model.service.SourceService;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Source Controller.
 */
@Transactional
@RestController
@RequestMapping("/v1/source")
public class SourceController {

  private SourceService sourceService;

  private final DSLContext ctx;

  @Autowired
  public SourceController(SourceService sourceService, DSLContext ctx) {
    this.sourceService = sourceService;
    this.ctx = ctx;
  }

  /**
   * Get all sources of the provided type.
   */
  @GetMapping
  public ResponseEntity getSourcesByTypes(
      @RequestParam(value = "type", required = false) String type) {
    try  {
      List<Source> sourceList;
      if (type == null || type.isEmpty()) {
        sourceList = sourceService.list(ctx);
      } else {
        sourceList = sourceService.listByType(ctx, SourceType.valueOf(type));
      }
      return new ResponseEntity<>(sourceList, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>("unknown type: " + type, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Get a source by its id.
   */
  @GetMapping("/{id}")
  public ResponseEntity getSource(@PathVariable("id") String sourceId) {
    try  {
      Source source = sourceService.read(ctx, Integer.parseInt(sourceId));
      if (source == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
      return new ResponseEntity<>(source, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Create a new source.
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public ResponseEntity addSource(@RequestBody Source source,
      UriComponentsBuilder uriComponentsBuilder,
      @RequestHeader(value = HttpHeaders.HOST, required = false) String host,
      @RequestHeader(value = "x-forwarded-proto", required = false) String scheme) {
    try  {
      int sourceId = sourceService.create(ctx, source);
      UriComponents uriComponents;
      if (host != null && scheme != null) {
        uriComponents =
            uriComponentsBuilder.path("/v1/source/{id}")
                .host(host)
                .scheme(scheme)
                .buildAndExpand(sourceId);
      } else {
        uriComponents =
            uriComponentsBuilder.path("/v1/source/{id}")
                .buildAndExpand(sourceId);
      }

      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setLocation(uriComponents.toUri());
      return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
    } catch (DataAccessException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

}
