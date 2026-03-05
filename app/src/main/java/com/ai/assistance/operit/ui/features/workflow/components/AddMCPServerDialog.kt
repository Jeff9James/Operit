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
    onConfirm: (serverName: String, endpoint: String, isRemote: Boolean) -> Unit
) {
    var serverName by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var isRemote by remember { mutableStateOf(true) } // true = HTTP/SSE, false = stdio
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

                // Connection Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = isRemote,
                                onClick = { isRemote = true }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isRemote,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.workflow_mcp_server_type_remote))
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = !isRemote,
                                onClick = { isRemote = false }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isRemote,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.workflow_mcp_server_type_stdio))
                    }
                }

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
                        onConfirm(serverName.trim(), endpoint.trim(), isRemote)
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
