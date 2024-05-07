/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.admin;

import static org.ehrbase.api.rest.HttpRestContext.COMPOSITION_ID;
import static org.ehrbase.api.rest.HttpRestContext.EHR_ID;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.rest.HttpRestContext;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.openehr.sdk.response.dto.admin.AdminDeleteResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API controller for Composition related data. Provides endpoint to remove compositions physically from database.
 */
@ConditionalOnMissingBean(name = "primaryadmincompositioncontroller")
@ConditionalOnProperty(prefix = "admin-api", name = "active")
@Tag(name = "Admin - Composition")
@RestController
@RequestMapping(
        path = BaseController.ADMIN_API_CONTEXT_PATH + "/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class AdminCompositionController extends BaseController {

    private final EhrService ehrService;
    private final CompositionService compositionService;

    @Autowired
    public AdminCompositionController(EhrService ehrService, CompositionService compositionService) {
        this.ehrService = Objects.requireNonNull(ehrService);
        this.compositionService = Objects.requireNonNull(compositionService);
    }

    @DeleteMapping(path = "/{ehr_id}/composition/{composition_id}")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Composition has been deleted successfully.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        }),
                @ApiResponse(
                        responseCode = "401",
                        description = "Client credentials are invalid or have been expired."),
                @ApiResponse(
                        responseCode = "403",
                        description = "Client has not permission to access this resource since admin role is missing."),
                @ApiResponse(responseCode = "404", description = "EHR or Composition with id could not be found.")
            })
    public ResponseEntity<AdminDeleteResponseData> deleteComposition(
            @Parameter(description = "Target EHR id to remove composition from", required = true)
                    @PathVariable(value = "ehr_id")
                    String ehrId,
            @Parameter(description = "Target Composition id to remove", required = true)
                    @PathVariable(value = "composition_id")
                    String compositionId) {
        UUID ehrUuid = UUID.fromString(ehrId);

        // Check if EHR exists
        if (!ehrService.hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException(
                    "Admin Composition", String.format("EHR with id %s does not exist.", ehrId));
        }

        UUID compositionUid = UUID.fromString(compositionId);

        compositionService.adminDelete(compositionUid);

        HttpRestContext.register(
                EHR_ID,
                ehrUuid,
                COMPOSITION_ID,
                compositionUid,
                HttpRestContext.LOCATION,
                fromPath("/ehr/{ehr_id}/composition/{composition_id}")
                        .build(ehrId, compositionId)
                        .toString());

        return ResponseEntity.noContent().build();
    }
}
