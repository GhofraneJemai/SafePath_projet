package com.example.safepath.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.safepath.R;
import com.example.safepath.fragments.ReportsHistoryFragment;
import com.example.safepath.fragments.SOSHistoryFragment;
import com.example.safepath.utils.HistoryPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HistoryActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private HistoryPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initViews();
        setupViewPager();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        pagerAdapter = new HistoryPagerAdapter(this);

        // CORRECTION : Utiliser les fragments du package fragments
        pagerAdapter.addFragment(new ReportsHistoryFragment(), "Signalements");
        pagerAdapter.addFragment(new SOSHistoryFragment(), "Alertes SOS");

        viewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))
        ).attach();
    }
}