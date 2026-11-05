package com.example.actividad1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.actividad1.FeedReaderContract.FeedEntry;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText nombre;
    private EditText apellido;
    private FeedReaderDbHelper database;
    private long selectedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        database = new FeedReaderDbHelper(this);
        nombre = findViewById(R.id.nombre);
        apellido = findViewById(R.id.apellido);
        if (savedInstanceState != null) {
            selectedId = savedInstanceState.getLong("selectedId", -1);
        }
        findViewById(R.id.btn_crear).setOnClickListener(view -> guardar(false));
        findViewById(R.id.btn_consultar).setOnClickListener(view -> consultar());
        findViewById(R.id.btn_actualizar).setOnClickListener(view -> guardar(true));
        findViewById(R.id.btn_eliminar).setOnClickListener(view -> confirmarEliminacion());
    }

    private void guardar(boolean actualizar) {
        if (actualizar && !haySeleccion()) {
            return;
        }

        String nombreIngresado = nombre.getText().toString().trim();
        String apellidoIngresado = apellido.getText().toString().trim();
        if (nombreIngresado.isEmpty()) {
            nombre.setError("Escribe el nombre");
            nombre.requestFocus();
            return;
        }
        if (apellidoIngresado.isEmpty()) {
            apellido.setError("Escribe el apellido");
            apellido.requestFocus();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_NOMBRE, nombreIngresado);
        values.put(FeedEntry.COLUMN_NAME_APELLIDO, apellidoIngresado);
        try {
            SQLiteDatabase db = database.getWritableDatabase();
            if (actualizar) {
                int updated = db.update(FeedEntry.TABLE_NAME, values, FeedEntry._ID + " = ?",
                        new String[]{Long.toString(selectedId)});
                if (updated == 0) {
                    selectedId = -1;
                    mostrarMensaje("El registro ya no existe");
                    return;
                }
            } else {
                db.insertOrThrow(FeedEntry.TABLE_NAME, null, values);
            }
            limpiarFormulario();
            mostrarMensaje(actualizar ? "Registro actualizado" : "Registro guardado");
        } catch (SQLiteException error) {
            mostrarMensaje("No se pudo guardar el registro");
        }
    }

    private void consultar() {
        List<Persona> personas = new ArrayList<>();
        try (Cursor cursor = database.getReadableDatabase().query(FeedEntry.TABLE_NAME,
                new String[]{FeedEntry._ID, FeedEntry.COLUMN_NAME_NOMBRE, FeedEntry.COLUMN_NAME_APELLIDO}, null, null, null, null, FeedEntry._ID + " DESC")) {
            while (cursor.moveToNext()) {
                personas.add(new Persona(cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
            }
        } catch (SQLiteException error) {
            mostrarMensaje("No se pudieron consultar los registros");
            return;
        }

        if (personas.isEmpty()) {
            mostrarMensaje("Todavía no hay registros");
            return;
        }
        CharSequence[] opciones = new CharSequence[personas.size()];
        for (int i = 0; i < personas.size(); i++) {
            Persona persona = personas.get(i);
            opciones[i] = "#" + persona.id + "  " + persona.nombre + " " + persona.apellido;
        }
        new AlertDialog.Builder(this)
                .setTitle("Selecciona un registro")
                .setItems(opciones, (dialog, index) -> {
                    Persona persona = personas.get(index);
                    selectedId = persona.id;
                    nombre.setText(persona.nombre);
                    apellido.setText(persona.apellido);
                    nombre.setError(null);
                    apellido.setError(null);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminacion() {
        if (!haySeleccion()) {
            return;
        }
        long id = selectedId;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar registro")
                .setMessage("¿Quieres eliminar el registro #" + id + "?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminar(id))
                .show();
    }

    private void eliminar(long id) {
        try {
            int deleted = database.getWritableDatabase().delete(FeedEntry.TABLE_NAME, FeedEntry._ID + " = ?",
                    new String[]{Long.toString(id)});
            limpiarFormulario();
            mostrarMensaje(deleted > 0 ? "Registro eliminado" : "El registro ya no existe");
        } catch (SQLiteException error) {
            mostrarMensaje("No se pudo eliminar el registro");
        }
    }

    private boolean haySeleccion() {
        if (selectedId != -1) {
            return true;
        }
        mostrarMensaje("Usa Consultar para seleccionar un registro");
        return false;
    }

    private void limpiarFormulario() {
        selectedId = -1;
        nombre.setText("");
        apellido.setText("");
        nombre.setError(null);
        apellido.setError(null);
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong("selectedId", selectedId);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private static class Persona {
        final long id;
        final String nombre;
        final String apellido;

        Persona(long id, String nombre, String apellido) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
        }
    }

}
