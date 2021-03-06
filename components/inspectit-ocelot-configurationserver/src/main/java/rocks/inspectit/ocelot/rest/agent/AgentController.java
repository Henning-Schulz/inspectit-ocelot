package rocks.inspectit.ocelot.rest.agent;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.agentstatus.AgentStatusManager;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.Map;


/**
 * The rest controller providing the interface used by the agent for configuration fetching.
 */
@RestController
@Slf4j
public class AgentController extends AbstractBaseController {

    @Autowired
    private AgentConfigurationManager configManager;

    @Autowired
    private AgentStatusManager statusManager;

    /**
     * Returns the {@link InspectitConfig} for the agent with the given name.
     * Uses text/plain as mime type to ensure that the configuration is presented nicely when opened in a browser
     *
     * @param attributes the attributes of the agents used to select the mapping
     * @return The configuration mapped on the given agent name
     */
    @ApiOperation(value = "Fetch the Agent Configuration", notes = "Reads the configuration for the given agent and returns it as a yaml string")
    @GetMapping(value = "agent/configuration", produces = "text/plain")
    public ResponseEntity<String> fetchConfiguration(@ApiParam("The agent attributes used to select the correct mapping") @RequestParam Map<String, String> attributes, @RequestHeader Map<String, String> headers) {
        log.debug("Fetching the agent configuration for agent ({})", attributes.toString());
        AgentConfiguration configuration = configManager.getConfiguration(attributes);
        statusManager.notifyAgentConfigurationFetched(attributes, headers, configuration);
        if (configuration == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.ok()
                    .eTag(configuration.getHash())
                    .body(configuration.getConfigYaml());
        }
    }
}
