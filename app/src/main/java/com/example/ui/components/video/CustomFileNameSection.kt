package com.example.ui.components.video

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CustomFileNameSection(
  fileName: String,
  targetExtension: String,
  onFileNameChange: (String) -> Unit
) {
  OutlinedTextField(
    value = fileName,
    onValueChange = onFileNameChange,
    label = { Text("Nombre del archivo de audio resultante") },
    suffix = { Text(".$targetExtension", fontWeight = FontWeight.Bold) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )
  )
}
