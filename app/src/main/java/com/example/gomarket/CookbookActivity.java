package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.gomarket.databinding.ActivityCookbookBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class CookbookActivity extends AppCompatActivity {

    private ActivityCookbookBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookbookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViewPager();
        setupClickListeners();
    }

    private void setupViewPager() {
        // Adapter for ViewPager2
        CookbookPagerAdapter adapter = new CookbookPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
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
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnAdd.setOnClickListener(v -> {
            // TODO: Mở màn hình tạo công thức mới
            startActivity(new Intent(this, AIChefActivity.class));
        });
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
