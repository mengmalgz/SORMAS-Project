package de.symeda.sormas.app.caze;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.person.ApproximateAgeType;
import de.symeda.sormas.api.symptoms.SymptomState;
import de.symeda.sormas.api.symptoms.SymptomsDto;
import de.symeda.sormas.api.symptoms.SymptomsHelper;
import de.symeda.sormas.api.symptoms.TemperatureSource;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.Diseases;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.caze.Case;
import de.symeda.sormas.app.backend.common.AbstractDomainObject;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.person.Person;
import de.symeda.sormas.app.backend.symptoms.Symptoms;
import de.symeda.sormas.app.component.CheckBoxField;
import de.symeda.sormas.app.component.FieldHelper;
import de.symeda.sormas.app.component.PropertyField;
import de.symeda.sormas.app.component.SymptomStateField;
import de.symeda.sormas.app.databinding.CaseSymptomsFragmentLayoutBinding;
import de.symeda.sormas.app.person.PersonProvider;
import de.symeda.sormas.app.util.DataUtils;
import de.symeda.sormas.app.util.FormTab;
import de.symeda.sormas.app.util.Item;
import de.symeda.sormas.app.validation.SymptomsValidator;


/**
 * Use this tab with arguments:
 * symptomsUuid as string
 * disease as serialized enum
 *
 * TODO move to symptoms package
 */
public class SymptomsEditForm extends FormTab {

    public static final String NEW_SYMPTOMS = "newSymptoms";
    public static final String FOR_VISIT = "forVisit";
    public static final String VISIT_COOPERATIVE = "visitCooperative";

    private CaseSymptomsFragmentLayoutBinding binding;
    private List<SymptomStateField> nonConditionalSymptoms;
    private List<SymptomStateField> conditionalBleedingSymptoms;
    private List<SymptomStateField> lesionsFields;
    private List<CheckBoxField> lesionsLocationFields;
    private List<SymptomStateField> monkeypoxFields;

    private boolean forVisit;
    private boolean visitCooperative;

    private Disease disease;
    private PersonProvider personProvider;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.case_symptoms_fragment_layout, container, false);

        View view = binding.getRoot();
        editOrCreateUserRight = (UserRight) getArguments().get(EDIT_OR_CREATE_USER_RIGHT);

        Symptoms symptoms;

        disease = (Disease) getArguments().getSerializable(Case.DISEASE);

        // build a new visit from contact data
        if(getArguments().getBoolean(NEW_SYMPTOMS)) {
            symptoms = DatabaseHelper.getSymptomsDao().build();
        }
        // open the given visit
        else {
            String symptomsUuid = getArguments().getString(Symptoms.UUID);
            symptoms = DatabaseHelper.getSymptomsDao().queryUuid(symptomsUuid);
        }

        if (getArguments().getBoolean(FOR_VISIT)) {
            forVisit = true;
            if (getArguments().getBoolean(VISIT_COOPERATIVE)) {
                visitCooperative = true;
            }
        }

        binding.setSymptoms(symptoms);

        nonConditionalSymptoms = Arrays.asList(binding.symptomsFever, binding.symptomsVomiting,
                binding.symptomsDiarrhea, binding.symptomsBloodInStool, binding.symptomsNausea, binding.symptomsAbdominalPain,
                binding.symptomsHeadache, binding.symptomsMusclePain, binding.symptomsFatigueWeakness, binding.symptomsUnexplainedBleeding,
                binding.symptomsSkinRash, binding.symptomsNeckStiffness, binding.symptomsSoreThroat, binding.symptomsCough,
                binding.symptomsRunnyNose, binding.symptomsDifficultyBreathing, binding.symptomsChestPain, binding.symptomsConfusedDisoriented,
                binding.symptomsSeizures, binding.symptomsAlteredConsciousness, binding.symptomsConjunctivitis,
                binding.symptomsEyePainLightSensitive, binding.symptomsKopliksSpots, binding.symptomsThrobocytopenia,
                binding.symptomsOtitisMedia, binding.symptomsHearingloss, binding.symptomsDehydration, binding.symptomsAnorexiaAppetiteLoss,
                binding.symptomsRefusalFeedorDrink, binding.symptomsJointPain, binding.symptomsShock,
                binding.symptomsHiccups, binding.symptomsBackache, binding.symptomsJaundice, binding.symptomsBulgingFontanelle,
                binding.symptomsDarkUrine, binding.symptomsRapidBreathing, binding.symptomsSwollenGlands,
                binding.symptomsOtherNonHemorrhagicSymptoms, binding.symptomsLesions, binding.symptomsLymphadenopathyAxillary,
                binding.symptomsLymphadenopathyCervical, binding.symptomsLymphadenopathyInguinal, binding.symptomsMeningealSigns,
                binding.symptomsChillsSweats, binding.symptomsBedridden,
                binding.symptomsOralUlcers, binding.symptomsBlackeningDeathOfTissue, binding.symptomsBuboesGroinArmpitNeck, binding.symptomsPainfulLymphadenitis);

        conditionalBleedingSymptoms = Arrays.asList(binding.symptomsGumsBleeding, binding.symptomsInjectionSiteBleeding,
                binding.symptomsNoseBleeding, binding.symptomsBloodyBlackStool, binding.symptomsRedBloodVomit,
                binding.symptomsDigestedBloodVomit, binding.symptomsCoughingBlood, binding.symptomsBleedingVagina,
                binding.symptomsSkinBruising, binding.symptomsBloodUrine, binding.symptomsEyesBleeding, binding.symptomsStomachBleeding,
                binding.symptomsOtherHemorrhagicSymptoms);

        binding.symptomsOnsetDate.initialize(this);
        binding.symptomsLesionsOnsetDate.initialize(this);
        binding.symptomsLesionsOnsetDate.makeFieldSoftRequired();

        List<Item> temperature = new ArrayList<>();
        temperature.add(new Item("",null));
        for (Float temperatureValue : SymptomsHelper.getTemperatureValues()) {
            temperature.add(new Item(SymptomsHelper.getTemperatureString(temperatureValue),temperatureValue));
        }

        FieldHelper.initSpinnerField(binding.symptomsTemperature, temperature);
        binding.symptomsTemperature.setSelectionOnOpen(37.0f);

        FieldHelper.initSpinnerField(binding.symptomsTemperatureSource, TemperatureSource.class);

        binding.symptomsUnexplainedBleeding.addValueChangedListener(new PropertyField.ValueChangeListener() {
            @Override
            public void onChange(PropertyField field) {
                updateUnexplainedBleedingFields();
                if (forVisit) {
                    SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
                    SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
                } else {
                    SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
                    SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
                }
            }
        });
        binding.symptomsOtherHemorrhagicSymptoms.addValueChangedListener(new PropertyField.ValueChangeListener() {
            @Override
            public void onChange(PropertyField field) {
                visibilityOtherHemorrhagicSymptoms();
                if (forVisit) {
                    SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
                    SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
                } else {
                    SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
                    SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
                }
            }
        });
        binding.symptomsOtherNonHemorrhagicSymptoms.addValueChangedListener(new PropertyField.ValueChangeListener() {
            @Override
            public void onChange(PropertyField field) {
                visibilityOtherNonHemorrhagicSymptoms();
                if (forVisit) {
                    SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
                    SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
                } else {
                    SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
                    SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
                }
            }
        });
        binding.symptomsLesions.addValueChangedListener(new PropertyField.ValueChangeListener() {
            @Override
            public void onChange(PropertyField field) {
                visibilityLesionsFields();
                if (forVisit) {
                    SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
                    SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
                } else {
                    SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
                    SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
                }
            }
        });

        lesionsFields = Arrays.asList(binding.symptomsLesionsSameState, binding.symptomsLesionsSameSize, binding.symptomsLesionsDeepProfound, binding.symptomsLesionsThatItch);

        lesionsLocationFields = Arrays.asList(binding.symptomsLesionsFace, binding.symptomsLesionsLegs, binding.symptomsLesionsSolesFeet, binding.symptomsLesionsPalmsHands,
                binding.symptomsLesionsThorax, binding.symptomsLesionsArms, binding.symptomsLesionsGenitals, binding.symptomsLesionsAllOverBody);

        monkeypoxFields = Arrays.asList(binding.symptomsLesionsResembleImg1, binding.symptomsLesionsResembleImg2, binding.symptomsLesionsResembleImg3, binding.symptomsLesionsResembleImg4);

        // set initial UI
        updateUnexplainedBleedingFields();
        visibilityOtherHemorrhagicSymptoms();
        visibilityOtherNonHemorrhagicSymptoms();
        visibilityLesionsFields();
        setVisibilityByDisease(SymptomsDto.class, disease, (ViewGroup)binding.getRoot());

        if (forVisit) {
            binding.symptomsPatientIllLocation.setVisibility(View.GONE);
        }

        FieldHelper.initSpinnerField(binding.symptomsOnsetSymptom, DataUtils.toItems(null, true));
        addListenerForOnsetSymptom();

        Button clearAllBtn = binding.symptomsClearAll;
        clearAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (SymptomStateField symptom : nonConditionalSymptoms) {
                    symptom.setValue(null);
                }
                for (SymptomStateField symptom : conditionalBleedingSymptoms) {
                    symptom.setValue(null);
                }
                for (SymptomStateField symptom : lesionsFields) {
                    symptom.setValue(null);
                }
                for (CheckBoxField checkBox : lesionsLocationFields) {
                    checkBox.setValue(null);
                }
                for (SymptomStateField symptom : monkeypoxFields) {
                    symptom.setValue(null);
                }
                if (forVisit) {
                    SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
                    SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
                } else {
                    SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
                    SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
                }
            }
        });

        Button setAllToNoBtn = binding.symptomsSetEmptyToNo;
        setAllToNoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (SymptomStateField symptom : nonConditionalSymptoms) {
                    if (symptom.getVisibility() == View.VISIBLE && symptom.getValue() == null) {
                        symptom.setValue(SymptomState.NO);
                    }
                }
                for (SymptomStateField symptom : conditionalBleedingSymptoms) {
                    if (symptom.getVisibility() == View.VISIBLE && symptom.getValue() == null) {
                        symptom.setValue(SymptomState.NO);
                    }
                }
                for (SymptomStateField symptom : lesionsFields) {
                    if (symptom.getVisibility() == View.VISIBLE && symptom.getValue() == null) {
                        symptom.setValue(SymptomState.NO);
                    }
                }
                for (SymptomStateField symptom : monkeypoxFields) {
                    if (symptom.getVisibility() == View.VISIBLE && symptom.getValue() == null) {
                        symptom.setValue(SymptomState.NO);
                    }
                }

                if (forVisit) {
                    SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
                    SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
                } else {
                    SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
                    SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
                }
            }
        });

        if (!forVisit) {
            SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
            SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
        } else {
            SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
            SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
        }

        // Add listeners to symptom state fields; OnWindowFocusChangeListener is used to make sure that these
        // listeners aren't called when the view is being built.
        binding.caseSymptomsForm.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.caseSymptomsForm.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                for (SymptomStateField symptom : nonConditionalSymptoms) {
                    symptom.addValueChangedListener(new PropertyField.ValueChangeListener() {
                        @Override
                        public void onChange(PropertyField field) {
                            if (forVisit) {
                                SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
                                SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(visitCooperative, binding);
                            } else {
                                SymptomsValidator.setRequiredHintsForCaseSymptoms(binding);
                                SymptomsValidator.setSoftRequiredHintsForCaseSymptoms(binding);
                            }
                        }
                    });
                }
            }
        });

        //view.requestFocus();
        return view;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            updateBulgingFontanelleVisibility();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // @TODO: Workaround, find a better solution. Remove autofocus on first field.
        getView().requestFocus();
    }

    private void visibilityOtherHemorrhagicSymptoms() {
        SymptomState symptomState = binding.symptomsOtherHemorrhagicSymptoms.getValue();
        binding.symptomsOtherHemorrhagicSymptomsLayout.setVisibility(symptomState == SymptomState.YES?View.VISIBLE:View.GONE);
        if(symptomState != SymptomState.YES) {
            binding.symptomsOther1HemorrhagicSymptomsText.setValue("");
        }
    }

    private void visibilityOtherNonHemorrhagicSymptoms() {
        SymptomState symptomState = binding.symptomsOtherNonHemorrhagicSymptoms.getValue();
        binding.symptomsOtherNonHemorrhagicSymptomsLayout.setVisibility(symptomState == SymptomState.YES?View.VISIBLE:View.GONE);
        if(symptomState != SymptomState.YES) {
            binding.symptomsOther1NonHemorrhagicSymptomsText.setValue("");
        }
    }

    private void visibilityLesionsFields() {
        SymptomState symptomState = binding.symptomsLesions.getValue();
        binding.symptomsLesionsLayout.setVisibility(symptomState == SymptomState.YES ? View.VISIBLE : View.GONE);
        binding.symptomsLesionsOnsetDate.setVisibility(symptomState == SymptomState.YES ? View.VISIBLE : View.GONE);
        if (symptomState != SymptomState.YES) {
            for (PropertyField field : lesionsFields) {
                field.setValue(null);
            }
            for (PropertyField field : lesionsLocationFields) {
                field.setValue(false);
            }

            binding.symptomsLesionsOnsetDate.setValue(null);
        }
        if (disease == Disease.MONKEYPOX) {
            binding.symptomsMonkeypoxLayout.setVisibility(symptomState == SymptomState.YES ? View.VISIBLE : View.GONE);
            if (symptomState != SymptomState.YES) {
                for (PropertyField field : monkeypoxFields) {
                    field.setValue(null);
                }
            }
        } else {
            binding.symptomsMonkeypoxLayout.setVisibility(View.GONE);
        }
    }

    private void updateUnexplainedBleedingFields() {
        SymptomState unexplainedBleeding = binding.symptomsUnexplainedBleeding.getValue();
        for (SymptomStateField field : conditionalBleedingSymptoms) {
            if (Diseases.DiseasesConfiguration.isDefinedOrMissing(SymptomsDto.class, field.getPropertyId(), disease)) {
                if (unexplainedBleeding == SymptomState.YES) {
                    setFieldVisible(field, true);
                } else {
                    field.setValue(null);
                    setFieldGone(field);
                }
            }
        }
    }

    private void addListenerForOnsetSymptom() {
        final ArrayAdapter<Item> adapter = (ArrayAdapter<Item>) binding.symptomsOnsetSymptom.getAdapter();
        List<SymptomStateField> relevantSymptoms = new ArrayList<>();
        relevantSymptoms.addAll(nonConditionalSymptoms);
        relevantSymptoms.addAll(conditionalBleedingSymptoms);
        relevantSymptoms.add(binding.symptomsLesionsThatItch);

        for (SymptomStateField symptom : relevantSymptoms) {
            symptom.addValueChangedListener(new PropertyField.ValueChangeListener() {
                @Override
                public void onChange(PropertyField field) {
                    Item item = new Item(field.getCaption(), field.getCaption());
                    int position = binding.symptomsOnsetSymptom.getPositionOf(item);
                    if (field.getValue() == SymptomState.YES) {
                        if (position == -1) {
                            adapter.add(item);
                        }
                    } else {
                        if (position != -1) {
                            adapter.remove(adapter.getItem(position));
                        }
                    }
                }
            });
        }
    }

    @Override
    public AbstractDomainObject getData() {
        return binding == null ? null : binding.getSymptoms();
    }

    public CaseSymptomsFragmentLayoutBinding getBinding() {
        return binding;
    }

    public void changeVisitCooperative(boolean cooperative) {
        visitCooperative = cooperative;
        SymptomsValidator.setRequiredHintsForVisitSymptoms(visitCooperative, binding);
        SymptomsValidator.setSoftRequiredHintsForVisitSymptoms(cooperative, binding);
    }

    /**
     * Make bulging fontanelle invisible when the person in question is older than one year
     */
    public void updateBulgingFontanelleVisibility() {
        if (binding == null || personProvider == null) {
            return;
        }
        Person person = personProvider.getPerson();

        boolean isInfant = person.getApproximateAge() != null
                && ((person.getApproximateAge() <= 12 && person.getApproximateAgeType() == ApproximateAgeType.MONTHS)
                || person.getApproximateAge() <= 1);
        if (isInfant && Diseases.DiseasesConfiguration.isDefinedOrMissing(SymptomsDto.class, binding.symptomsBulgingFontanelle.getPropertyId(), disease)) {
            binding.symptomsBulgingFontanelle.setVisibility(View.VISIBLE);
        } else {
            binding.symptomsBulgingFontanelle.setVisibility(View.GONE);
        }
    }

    public void setPersonProvider(PersonProvider personProvider) {
        if (this.personProvider != personProvider) {
            this.personProvider = personProvider;
            updateBulgingFontanelleVisibility();
        }
    }
}