
package com.cafeteria.pos;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private LinearLayout root, productsBox, cartBox;
    private TextView totalView, salesView;
    private final Map<String, Integer> cart = new LinkedHashMap<>();
    private final Map<String, Integer> prices = new LinkedHashMap<>();
    private SharedPreferences prefs;
    private int total = 0;

    private String money(int amount) {
        return String.format(Locale.getDefault(), "%,d CUP", amount);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences("cafeteria_pos", MODE_PRIVATE);

        prices.put("Cafe", 100);
        prices.put("Cafe con leche", 150);
        prices.put("Capuchino", 200);
        prices.put("Te", 80);
        prices.put("Jugo", 120);
        prices.put("Agua", 60);
        prices.put("Croissant", 180);
        prices.put("Sandwich", 300);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.rgb(250, 247, 240));

        TextView title = new TextView(this);
        title.setText("CAFETERIA POS");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(91, 55, 31));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 12, 0, 16);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Ventas sin internet | Moneda: CUP");
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        Button reportButton = new Button(this);
        reportButton.setText("REPORTE DE VENTAS");
        root.addView(reportButton);
        reportButton.setOnClickListener(v -> showReport());

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1));

        TextView productTitle = new TextView(this);
        productTitle.setText("\nPRODUCTOS");
        productTitle.setTextSize(19);
        content.addView(productTitle);

        productsBox = new LinearLayout(this);
        productsBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(productsBox);

        for (String name : prices.keySet()) {
            Button b = new Button(this);
            b.setText(name + "  -  " + money(prices.get(name)));
            productsBox.addView(b);
            b.setOnClickListener(v -> addProduct(name));
        }

        TextView cartTitle = new TextView(this);
        cartTitle.setText("\nCARRITO DE VENTA");
        cartTitle.setTextSize(19);
        content.addView(cartTitle);

        cartBox = new LinearLayout(this);
        cartBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(cartBox);

        totalView = new TextView(this);
        totalView.setTextSize(23);
        totalView.setTextColor(Color.rgb(91, 55, 31));
        totalView.setPadding(0, 12, 0, 12);
        content.addView(totalView);

        Button clear = new Button(this);
        clear.setText("VACIAR CARRITO");
        content.addView(clear);
        clear.setOnClickListener(v -> {
            cart.clear();
            refreshCart();
        });

        Button pay = new Button(this);
        pay.setText("COBRAR VENTA");
        content.addView(pay);
        pay.setOnClickListener(v -> choosePayment());

        salesView = new TextView(this);
        salesView.setGravity(Gravity.CENTER);
        salesView.setPadding(0, 12, 0, 12);
        root.addView(salesView);

        setContentView(root);
        refreshCart();
        updateSales();
    }

    private void addProduct(String name) {
        cart.put(name, cart.containsKey(name)
                ? cart.get(name) + 1 : 1);
        refreshCart();
    }

    private void refreshCart() {
        cartBox.removeAllViews();
        total = 0;

        for (String name : cart.keySet()) {
            int quantity = cart.get(name);
            int subtotal = prices.get(name) * quantity;
            total += subtotal;

            TextView row = new TextView(this);
            row.setText(name + " x " + quantity
                    + " = " + money(subtotal));
            row.setTextSize(16);
            row.setPadding(4, 8, 4, 8);
            cartBox.addView(row);

            Button remove = new Button(this);
            remove.setText("Quitar una unidad");
            cartBox.addView(remove);
            remove.setOnClickListener(v -> {
                int q = cart.get(name);
                if (q <= 1) cart.remove(name);
                else cart.put(name, q - 1);
                refreshCart();
            });
        }

        totalView.setText("TOTAL: " + money(total));
    }

    private void choosePayment() {
        if (cart.isEmpty() || total <= 0) {
            Toast.makeText(this, "Agrega productos primero",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] methods = {"Efectivo", "Tarjeta", "Transferencia"};

        new AlertDialog.Builder(this)
                .setTitle("Forma de pago")
                .setItems(methods, (dialog, which) ->
                        saveSale(methods[which]))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void saveSale(String method) {
        String date = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        String old = prefs.getString("sales", "");
        StringBuilder sale = new StringBuilder();
        sale.append(date).append(" | ")
                .append(method).append(" | ")
                .append(total).append(" CUP");

        for (String name : cart.keySet()) {
            sale.append(" | ").append(name)
                    .append(" x ").append(cart.get(name));
        }

        String updated = old.isEmpty()
                ? sale.toString()
                : old + "\n" + sale;

        prefs.edit()
                .putString("sales", updated)
                .putInt("last_total", total)
                .apply();

        cart.clear();
        refreshCart();
        updateSales();

        Toast.makeText(this, "Venta guardada: "
                + money(prefs.getInt("last_total", 0)),
                Toast.LENGTH_LONG).show();
    }

    private void updateSales() {
        String data = prefs.getString("sales", "");
        int count = 0;

        if (!data.isEmpty()) {
            for (String line : data.split("\n")) {
                if (!line.trim().isEmpty()) count++;
            }
        }

        salesView.setText("Ventas registradas: " + count);
    }

    private void showReport() {
        String data = prefs.getString("sales", "");

        if (data.isEmpty()) data = "Todavia no hay ventas.";

        TextView report = new TextView(this);
        report.setText(data);
        report.setTextSize(15);
        report.setPadding(20, 20, 20, 20);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(report);

        new AlertDialog.Builder(this)
                .setTitle("Historial de ventas")
                .setView(scroll)
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
