package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CookbookActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookbook);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        setupViewPager();
        setupClickListeners();
    }

    private void setupViewPager() {
        CookbookPagerAdapter adapter = new CookbookPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("💡 Gợi Ý");
                    break;
                case 1:
                    tab.setText("🌍 Cộng Đồng");
                    break;
                case 2:
                    tab.setText("👤 Của Tôi");
                    break;
            }
        }).attach();
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AIChefActivity.class)));
    }

    // Adapter for ViewPager2
    private static class CookbookPagerAdapter extends FragmentStateAdapter {

        public CookbookPagerAdapter(FragmentActivity activity) {
            super(activity);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new SuggestionFragment();
                case 1:
                    return new CommunityFragment();
                case 2:
                    return new PersonalFragment();
                default:
                    return new SuggestionFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
