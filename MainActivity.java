package club.wello.mmusic;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import club.wello.mmusic.ui.BaseActivity;
import club.wello.mmusic.util.BottomNavigationViewHelper;

public class MainActivity extends BaseActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.bnv)
    BottomNavigationView bottomNavigationView;
    @BindView(R.id.view_pager)
    ViewPager viewPager;

    private MenuItem menuItem;
    private String[] fragmentNames = new String[4];
    private static final int NUM_PAGES = 4;
    private Fragment[] fragments = new Fragment[4];
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragmentNames = getResources().getStringArray(R.array.fragment_names);
        fragmentManager = getSupportFragmentManager();
        initView();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private void initView() {
        ButterKnife.bind(this);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (menuItem != null) {
                    menuItem.setChecked(false);
                } else {
                    bottomNavigationView.getMenu().getItem(0).setChecked(false);
                }
                menuItem = bottomNavigationView.getMenu().getItem(position);
                menuItem.setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.library:
                        viewPager.setCurrentItem(0);
                        return true;
                    case R.id.search:
                        viewPager.setCurrentItem(1);
                        return true;
                    case R.id.for_you:
                        viewPager.setCurrentItem(2);
                        return true;
                    case R.id.radio:
                        viewPager.setCurrentItem(3);
                        return false;
                }
                return false;
            }
        });
        viewPager.setAdapter(new FragmentPagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                return chooseFragment(position);
            }

            @Override
            public int getCount() {
                return NUM_PAGES;
            }
        });
    }

    private Fragment chooseFragment(int position) {
        if (fragments[position] == null) {
            switch (position) {
                case 0:
                    fragments[position] = new LibraryFragment();
                    return fragments[position];
                case 1:
                    fragments[position] = new SearchFragment();
                    return fragments[position];
                case 2:
                    fragments[position] = new ForYouFragment();
                    return fragments[position];
                case 3:
                    fragments[position] = new RadioFragment();
                    return fragments[position];
                default:
                    return null;
            }
        } else {
            return fragments[position];
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
