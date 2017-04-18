package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Mattaniah on 6/25/2015.
 */
public class AlertFragment extends Fragment {
    HostActivity hostActivity;
    Activity context;
    AlertHelper alertHelper;
    List<AlertHelper.Alert> allAlerts = new ArrayList<>();
    CoordinatorLayout mainView;
    RecyclerView alertsContainer;
    AlertsViewAdapter adapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
        this.hostActivity = (HostActivity) activity;
        this.alertHelper = new AlertHelper(activity);
        allAlerts = alertHelper.getSavedAlerts();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = (CoordinatorLayout) inflater.inflate(R.layout.bookmarks_fragment_layout, null);
        hostActivity.getToolbar().setTitle("עתים קבועים");
        hostActivity.getToolbar().findViewById(R.id.rightDrawerToggle).setVisibility(View.GONE);
        hostActivity.getToolbar().findViewById(R.id.toolbarSearchView).setVisibility(View.GONE);
        hostActivity.getTablayout().removeAllTabs();
        hostActivity.getTablayout().setVisibility(View.GONE);
        alertsContainer = (RecyclerView) mainView.findViewById(R.id.bookmarksContainer);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        alertsContainer.setLayoutManager(linearLayoutManager);
        final FloatingActionButton addAlert = (FloatingActionButton) mainView.findViewById(R.id.deleteAll);
        addAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertHelper.Alert alert = alertHelper.getDefaultAlert();
                alert.setActivated(true);
                allAlerts.add(alert);
                adapter.notifyItemInserted(allAlerts.size());
            }
        });
        addAlert.setImageResource(R.drawable.ic_add_white);
        adapter = new AlertsViewAdapter();
        alertsContainer.setAdapter(adapter);
        return mainView;
    }

    @Override
    public void onPause() {
        super.onPause();
        alertHelper.saveListToJSON(allAlerts);
    }

    private class AlertsViewAdapter extends RecyclerView.Adapter<AlertsViewAdapter.ViewHolder> {
        private final String timePattern = "H:mm";

        public AlertsViewAdapter() {
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            View view;
            TextView time;
            SwitchCompat activatedSwitch;
            Spinner personaSpinner;
            View delete;
            View divider;

            public ViewHolder(View v) {
                super(v);
                this.view = v;
                time = (TextView) view.findViewById(R.id.alarmTime);
                activatedSwitch = (SwitchCompat) view.findViewById(R.id.activatedSwitch);
                personaSpinner = (Spinner) view.findViewById(R.id.personaSpiner);
                delete = view.findViewById(R.id.deleteAlert);
                divider = view.findViewById(R.id.divider);

                personaSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, AlertHelper.Personas.values()));
            }
        }

        @Override
        public AlertsViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(View.inflate(getActivity(), R.layout.alert_item_view, null));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final AlertHelper.Alert alert = allAlerts.get(position);
            Calendar time = Calendar.getInstance();
            time.set(Calendar.HOUR_OF_DAY, alert.hourOfDay);
            time.set(Calendar.MINUTE, alert.minuteOfHour);
            holder.time.setText(new SimpleDateFormat(timePattern).format(time.getTime()));
            holder.activatedSwitch.setChecked(alert.isActivated());
            holder.divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            AlertHelper.Personas[] personaValues = AlertHelper.Personas.values();
            for (int i = 0; i < personaValues.length; i++) {
                if (personaValues[i] == alert.persona)
                    holder.personaSpinner.setSelection(i);
            }
            holder.personaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    alert.persona = AlertHelper.Personas.values()[position];
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            holder.time.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            alert.hourOfDay = hourOfDay;
                            alert.minuteOfHour = minute;
                        }
                    }, alert.hourOfDay, alert.minuteOfHour, false);
                    timePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Calendar time = Calendar.getInstance();
                            time.set(Calendar.HOUR_OF_DAY, alert.hourOfDay);
                            time.set(Calendar.MINUTE, alert.minuteOfHour);
                            holder.time.setText(new SimpleDateFormat(timePattern).format(time.getTime()));
                        }
                    });
                    timePickerDialog.show();
                }
            });

            holder.activatedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    alert.setActivated(isChecked);
                }
            });

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    allAlerts.remove(alert);
                    AlertHelper.deleteAlert(alert, context);
                    adapter.notifyItemRemoved(position);
                    Snackbar.make(mainView, "Alert Deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    allAlerts.add(alert);
                                    adapter.notifyItemInserted(position);
                                }
                            })
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return allAlerts.size();
        }
    }
}
