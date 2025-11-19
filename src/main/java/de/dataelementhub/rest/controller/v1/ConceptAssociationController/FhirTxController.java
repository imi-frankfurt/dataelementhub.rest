package de.dataelementhub.rest.controller.v1.ConceptAssociationController;

import de.dataelementhub.model.service.Terminologies.Concept;
import de.dataelementhub.model.service.Terminologies.FhirTxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;

@RestController
@RequestMapping(API_VERSION +"/fhirTx")
public class FhirTxController {
    private final FhirTxService fhirTxService;

    @Autowired
    public FhirTxController(FhirTxService fhirTxService) {
        this.fhirTxService = fhirTxService;
    }

    @GetMapping("/searchTerm")
    public List<Concept> searchTermFhir(@RequestParam String query) {
        return fhirTxService.searchFhir(query);
    }
}
