package de.dataelementhub.rest.controller.v1.ConceptAssociationController;

import de.dataelementhub.model.service.SourceService;
import de.dataelementhub.model.service.Terminologies.DefinedVDService;
import de.dataelementhub.model.service.Terminologies.LoincService;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/v1/loincFHIR")
public class DefinedValueDomainController {
    private final DefinedVDService definedVDService;

    @Autowired
    public DefinedValueDomainController(DefinedVDService definedVDService) {
        this.definedVDService = definedVDService;
    }
    /**
     * Search LOINC ValueSet using the FHIR API by free text.
     * Example: GET /v1/loinc/loincFHI/searchValueSet?query=diabetes
     */
    @GetMapping("/searchValueSet")
    public List<DefinedVDService.ValueSetResponse> searchValueSet(@RequestParam String query) {
        return definedVDService.loincValueSet(query);
    }




}
