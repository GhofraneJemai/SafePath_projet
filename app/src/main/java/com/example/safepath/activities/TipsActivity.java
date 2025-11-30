package com.example.safepath.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.adapters.TipsAdapter;
import com.example.safepath.models.SecurityTip;

import java.util.ArrayList;
import java.util.List;

public class TipsActivity extends AppCompatActivity {
    private RecyclerView tipsRecyclerView;
    private TipsAdapter adapter;
    private List<SecurityTip> tips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        initViews();
        loadTips();
    }

    private void initViews() {
        tipsRecyclerView = findViewById(R.id.tipsRecyclerView);
        tipsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        tips = new ArrayList<>();
        adapter = new TipsAdapter(tips);
        tipsRecyclerView.setAdapter(adapter);

        // Bouton retour
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Bouton ressources externes
        findViewById(R.id.resourcesButton).setOnClickListener(v -> openExternalResources());
    }

    private void loadTips() {
        // Utiliser seulement les drawables de base qui existent
        tips.add(new SecurityTip("Déplacement de nuit",
                "• Privilégiez les rues éclairées et fréquentées\n• Évitez les raccourcis par des ruelles sombres\n• Restez attentif à votre environnement\n• Avertissez quelqu'un de votre itinéraire\n• Gardez votre téléphone chargé et accessible",
                R.drawable.ic_night));

        tips.add(new SecurityTip("Transports publics",
                "• Asseyez-vous près du conducteur ou dans des zones éclairées\n• Évitez de montrer vos objets de valeur\n• Restez dans des zones éclairées en attendant\n• Notez les numéros d'urgence du transport\n• Faites attention à votre environnement en descendant",
                R.drawable.ic_transport));

        tips.add(new SecurityTip("Lieux isolés",
                "• Avertissez toujours quelqu'un de votre destination\n• Gardez votre téléphone chargé avec batterie externe\n• Repérez les sorties et points de secours\n• Évitez les écouteurs pour rester attentif\n• Ayez un sifflet ou alarme personnelle",
                R.drawable.ic_isolated));

        tips.add(new SecurityTip("Sécurité numérique",
                "• Ne partagez pas votre position en temps réel sur les réseaux\n• Utilisez des mots de passe forts pour vos applications\n• Activez l'authentification à deux facteurs\n• Vérifiez les paramètres de confidentialité\n• Méfiez-vous des Wi-Fi publics non sécurisés",
                R.drawable.ic_shield)); // Utiliser ic_shield existant

        tips.add(new SecurityTip("Conduite automobile",
                "• Verrouillez toujours vos portières\n• Garez-vous dans des endroits éclairés\n• Vérifiez l'arrière de votre véhicule avant de monter\n• Ayez votre téléphone chargé et un chargeur d'urgence\n• Connaissez les numéros d'assistance routière",
                R.drawable.ic_route)); // Utiliser ic_route existant

        tips.add(new SecurityTip("Réseaux sociaux",
                "• Ne publiez pas vos déplacements en temps réel\n• Évitez de géolocaliser votre domicile\n• Utilisez des paramètres de confidentialité stricts\n• Méfiez-vous des demandes d'amis inconnus\n• Signalez les comportements suspects",
                R.drawable.ic_profile)); // Utiliser ic_profile existant

        adapter.notifyDataSetChanged();
    }

    private void openExternalResources() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.service-public.fr/particuliers/vosdroits/F13529"));
        startActivity(browserIntent);
    }
}