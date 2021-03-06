/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.app.caze.read;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.ObservableArrayList;

import de.symeda.sormas.api.epidata.EpiDataDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.Diseases;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.app.BaseReadFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.caze.Case;
import de.symeda.sormas.app.backend.epidata.EpiData;
import de.symeda.sormas.app.backend.epidata.EpiDataBurial;
import de.symeda.sormas.app.backend.epidata.EpiDataGathering;
import de.symeda.sormas.app.backend.epidata.EpiDataTravel;
import de.symeda.sormas.app.component.controls.ValueChangeListener;
import de.symeda.sormas.app.component.dialog.InfoDialog;
import de.symeda.sormas.app.core.FieldHelper;
import de.symeda.sormas.app.core.IEntryItemOnClickListener;
import de.symeda.sormas.app.databinding.FragmentCaseReadEpidLayoutBinding;
import de.symeda.sormas.app.util.DiseaseConfigurationCache;

public class CaseReadEpidemiologicalDataFragment extends BaseReadFragment<FragmentCaseReadEpidLayoutBinding, EpiData, Case> {

	public static final String TAG = CaseReadEpidemiologicalDataFragment.class.getSimpleName();

	private EpiData record;

	private IEntryItemOnClickListener onBurialItemClickListener;
	private IEntryItemOnClickListener onGatheringItemClickListener;
	private IEntryItemOnClickListener onTravelItemClickListener;

	// Static methods

	public static CaseReadEpidemiologicalDataFragment newInstance(Case activityRootData) {
		return newInstanceWithFieldCheckers(
			CaseReadEpidemiologicalDataFragment.class,
			null,
			activityRootData,
			FieldVisibilityCheckers.withDisease(activityRootData.getDisease()),
			null);
	}

	// Instance methods

	private void setUpControlListeners() {
		onBurialItemClickListener = new IEntryItemOnClickListener() {

			@Override
			public void onClick(View v, Object item) {
				InfoDialog infoDialog = new InfoDialog(getContext(), R.layout.dialog_case_epid_burial_read_layout, item);
				infoDialog.show();
			}
		};

		onGatheringItemClickListener = new IEntryItemOnClickListener() {

			@Override
			public void onClick(View v, Object item) {
				InfoDialog infoDialog = new InfoDialog(getContext(), R.layout.dialog_case_epid_gathering_read_layout, item);
				infoDialog.show();
			}
		};

		onTravelItemClickListener = new IEntryItemOnClickListener() {

			@Override
			public void onClick(View v, Object item) {
				InfoDialog infoDialog = new InfoDialog(getContext(), R.layout.dialog_case_epid_travel_read_layout, item);
				infoDialog.show();
			}
		};
	}

	// Overrides

	@Override
	protected void prepareFragmentData(Bundle savedInstanceState) {
		Case caze = getActivityRootData();
		record = caze.getEpiData();
	}

	@Override
	public void onLayoutBinding(FragmentCaseReadEpidLayoutBinding contentBinding) {
		setUpControlListeners();

		ObservableArrayList<EpiDataBurial> burials = new ObservableArrayList<>();
		burials.addAll(record.getBurials());
		ObservableArrayList<EpiDataTravel> travels = new ObservableArrayList<>();
		travels.addAll(record.getTravels());
		ObservableArrayList<EpiDataGathering> gatherings = new ObservableArrayList<>();
		gatherings.addAll(record.getGatherings());

		contentBinding.setData(record);
		contentBinding.setBurialList(burials);
		contentBinding.setGatheringList(gatherings);
		contentBinding.setTravelList(travels);
		contentBinding.setBurialItemClickCallback(onBurialItemClickListener);
		contentBinding.setGatheringItemClickCallback(onGatheringItemClickListener);
		contentBinding.setTravelItemClickCallback(onTravelItemClickListener);

		// iterate through all epi data animal fields and add listener
		ValueChangeListener updateHadAnimalExposureListener = field -> updateHadAnimalExposure();
		List<String> animalExposureProperties = Arrays.asList(EpiDataDto.ANIMAL_EXPOSURE_PROPERTIES);
		FieldHelper.iteratePropertyFields((ViewGroup) contentBinding.getRoot(), field -> {
			if (animalExposureProperties.contains(field.getSubPropertyId())) {
				field.addValueChangedListener(updateHadAnimalExposureListener);
			}
			return true;
		});

		List<String> environmentalExposureProperties = Arrays.asList(EpiData.ENVIRONMENTAL_EXPOSURE_PROPERTIES);
		int environmentalExposureHeadingVisibiliy = View.GONE;
		for (String property : environmentalExposureProperties) {
			if (Diseases.DiseasesConfiguration.isDefinedOrMissing(EpiDataDto.class, property, getActivityRootData().getDisease())) {
				environmentalExposureHeadingVisibiliy = View.VISIBLE;
				break;
			}
		}
		contentBinding.environmentalExposureDivider.setVisibility(environmentalExposureHeadingVisibiliy);
		contentBinding.headingEnvironmentalExposure.setVisibility(environmentalExposureHeadingVisibiliy);
	}

	private void updateHadAnimalExposure() {
		// iterate through all epi data animal fields to get value
		List<String> animalExposureProperties = Arrays.asList(EpiDataDto.ANIMAL_EXPOSURE_PROPERTIES);
		boolean iterationCancelled = !FieldHelper.iteratePropertyFields((ViewGroup) getContentBinding().getRoot(), field -> {
			if (animalExposureProperties.contains(field.getSubPropertyId())) {
				YesNoUnknown value = (YesNoUnknown) field.getValue();
				if (YesNoUnknown.YES.equals(value)) {
					return false;
				}
			}
			return true;
		});
		boolean hadAnimalExposure = iterationCancelled;
		getContentBinding().setAnimalExposureDependentVisibility(hadAnimalExposure ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onAfterLayoutBinding(FragmentCaseReadEpidLayoutBinding contentBinding) {
		setFieldVisibilitiesAndAccesses(EpiDataDto.class, contentBinding.mainContent);

		if (DiseaseConfigurationCache.getInstance().getFollowUpDuration(getActivityRootData().getDisease()) > 0) {
			contentBinding.epiDataTraveled.setCaption(
				String.format(
					I18nProperties.getCaption(Captions.epiDataTraveledIncubationPeriod),
					DiseaseConfigurationCache.getInstance().getFollowUpDuration(getActivityRootData().getDisease())));
		}
	}

	@Override
	protected String getSubHeadingTitle() {
		return getResources().getString(R.string.caption_case_epidemiological_data);
	}

	@Override
	public EpiData getPrimaryData() {
		return record;
	}

	@Override
	public int getReadLayout() {
		return R.layout.fragment_case_read_epid_layout;
	}
}
