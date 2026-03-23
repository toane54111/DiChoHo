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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookbook);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new CookbookPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Gợi Ý"); break;
                case 1: tab.setText("Cộng Đồng"); break;
                case 2: tab.setText("Của Tôi"); break;
            }
        }).attach();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddRecipe).setOnClickListener(v ->
                startActivity(new Intent(this, CreateCookbookRecipeActivity.class)));
    }

    private static class CookbookPagerAdapter extends FragmentStateAdapter {
        CookbookPagerAdapter(FragmentActivity activity) { super(activity); }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return CookbookListFragment.newInstance("suggestions");
                case 1: return CookbookListFragment.newInstance("community");
                case 2: return CookbookListFragment.newInstance("personal");
                default: return CookbookListFragment.newInstance("suggestions");
            }
        }

        @Override
        public int getItemCount() { return 3; }
    }
}
