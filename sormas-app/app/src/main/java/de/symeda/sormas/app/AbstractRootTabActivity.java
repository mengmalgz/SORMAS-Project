package de.symeda.sormas.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.backend.user.User;
import de.symeda.sormas.app.caze.CaseNewActivity;
import de.symeda.sormas.app.caze.CasesActivity;
import de.symeda.sormas.app.contact.ContactsActivity;
import de.symeda.sormas.app.event.EventsActivity;
import de.symeda.sormas.app.reports.ReportsActivity;
import de.symeda.sormas.app.rest.SynchronizeDataAsync;
import de.symeda.sormas.app.sample.SamplesActivity;
import de.symeda.sormas.app.settings.SettingsActivity;
import de.symeda.sormas.app.task.TasksActivity;

public abstract class AbstractRootTabActivity extends AbstractTabActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActionBarDrawerToggle menuDrawerToggle;
    private DrawerLayout menuDrawerLayout;
    private String[] menuTitles;
    private NavigationView navigationView;

    private CharSequence mainViewTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);

        setupDrawer();
    }

    private void setupDrawer() {

        menuDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        menuDrawerToggle = new ActionBarDrawerToggle(
                this,
                menuDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getSupportActionBar().setTitle("");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getSupportActionBar().setTitle(mainViewTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        menuDrawerToggle.setDrawerIndicatorEnabled(true);
        menuDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_drawer);
        menuDrawerLayout.addDrawerListener(menuDrawerToggle);

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        // adjust visible menu entries based on user role
        User user = ConfigProvider.getUser();
        if (user != null ) {
            Menu menu = navigationView.getMenu();
            menu.findItem(R.id.nav_tasks).setVisible(user.hasUserRight(UserRight.TASK_VIEW));
            menu.findItem(R.id.nav_cases).setVisible(user.hasUserRight(UserRight.CASE_VIEW));
            menu.findItem(R.id.nav_samples).setVisible(user.hasUserRight(UserRight.SAMPLE_VIEW));
            menu.findItem(R.id.nav_events).setVisible(user.hasUserRight(UserRight.EVENT_VIEW));
            menu.findItem(R.id.nav_contacts).setVisible(user.hasUserRight(UserRight.CONTACT_VIEW));
            menu.findItem(R.id.nav_reports).setVisible(user.hasUserRight(UserRight.WEEKLYREPORT_VIEW));

            // replace empty user sub header with user name and role
            String username = ConfigProvider.getUser().toString();
            menu.findItem(R.id.navigation_user_sub_header).setTitle(username);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle params = getIntent().getExtras();
        if(params!=null) {
            if (params.containsKey(KEY_PAGE)) {
                currentTab = params.getInt(KEY_PAGE);
            }
        }

        synchronizeData(SynchronizeDataAsync.SyncMode.Changes, false, false, false, null, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_PAGE, currentTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (menuDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mainViewTitle = title;
        getSupportActionBar().setTitle(mainViewTitle);
    }

    public void showCasesView() {
        Intent intent = new Intent(this, CasesActivity.class);
        startActivity(intent);
    }

    public void showCaseNewView() {
        Intent intent = new Intent(this, CaseNewActivity.class);
        startActivity(intent);
    }

    public void showContactsView() {
        Intent intent = new Intent(this, ContactsActivity.class);
        startActivity(intent);
    }

    public void showTasksView() {
        Intent intent = new Intent(this, TasksActivity.class);
        startActivity(intent);
    }

    public void showEventsView() {
        Intent intent = new Intent(this, EventsActivity.class);
        startActivity(intent);
    }

    public void showSamplesView() {
        Intent intent = new Intent(this, SamplesActivity.class);
        startActivity(intent);
    }

    public void showReportsView() {
        Intent intent = new Intent(this, ReportsActivity.class);
        startActivity(intent);
    }

    public void showSettingsView() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_tasks:
                showTasksView();
                break;
            case R.id.nav_cases:
                showCasesView();
                break;
            case R.id.nav_contacts:
                showContactsView();
                break;
            case R.id.nav_events:
                showEventsView();
                break;
            case R.id.nav_samples:
                showSamplesView();
                break;
            case R.id.nav_reports:
                showReportsView();
                break;
            case R.id.nav_settings:
                showSettingsView();
                break;
            case R.id.nav_syncAll:
                synchronizeCompleteData();
                break;
            default:
                throw new IndexOutOfBoundsException("No action defined for menu entry: " + item.getItemId());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}