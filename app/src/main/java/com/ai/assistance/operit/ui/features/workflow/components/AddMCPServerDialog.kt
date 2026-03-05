package com.ai.assistance.operit.ui.features.workflow.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R

/**
 * Dialog for adding a new MCP server to the workflow
 */
@Composable
fun AddMCPServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (serverName: String, endpoint: String) -> Unit
) {
    var serverName by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var serverNameError by remember { mutableStateOf(false) }
    var endpointError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workflow_mcp_add_server)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Server Name
                OutlinedTextField(
                    value = serverName,
                    onValueChange = {
                        serverName = it
                        serverNameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.workflow_mcp_server_name)) },
                    placeholder = { Text(stringResource(R.string.workflow_mcp_server_name_hint)) },
                    isError = serverNameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Server Endpoint
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        endpointError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.workflow_mcp_server_endpoint)) },
                    placeholder = { Text(stringResource(R.string.workflow_mcp_server_endpoint_hint)) },
                    isError = endpointError,
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    serverNameError = serverName.isBlank()
                    endpointError = endpoint.isBlank()

                    if (!serverNameError && !endpointError) {
                        onConfirm(serverName.trim(), endpoint.trim())
                    }
                }
            ) {
                Text(stringResource(R.string.confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}
