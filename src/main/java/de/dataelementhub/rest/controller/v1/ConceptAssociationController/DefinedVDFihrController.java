package de.dataelementhub.rest.controller.v1.ConceptAssociationController;

import de.dataelementhub.model.service.Terminologies.DefinedVDFihrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/fhir")
public class DefinedVDFihrController {
    private final DefinedVDFihrService definedVDFihrService;
    @Autowired
    public DefinedVDFihrController(DefinedVDFihrService definedVDService) {
        this.definedVDFihrService = definedVDService;
    }
    @GetMapping
    public ResponseEntity<List<DefinedVDFihrService.ValueSetResponse>> getValueSets(
            @RequestParam String query,
            @RequestParam String baseUrl,
            @RequestParam(defaultValue = "name:in") String queryParam,
            @RequestParam(defaultValue = "false") boolean translate,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set("Authorization", authHeader);
        }

        List<DefinedVDFihrService.ValueSetResponse> results = definedVDFihrService.fetchValueSet(
                query,
                baseUrl,
                queryParam,
                translate,
                headers
        );

        return ResponseEntity.ok(results);
    }
}
