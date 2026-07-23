document.addEventListener("DOMContentLoaded", function() {
    
    const cinInput = document.getElementById("cinInput");
    const companyName = document.getElementById("companyName");
    const companyAddress = document.getElementById("companyAddress");
    const companyEmail = document.getElementById("companyEmail");
    
    // Simulate API fetch when a valid CIN is entered
    if (cinInput) {
        cinInput.addEventListener("blur", function() {
            if(cinInput.value.length > 5) {
                // Mock Data
                companyName.value = "KHEKRA MINES PRIVATE LIMITED";
                companyAddress.value = "RZ-F-70, UGF, Khasra No. 84/8/1, Mahavir Er";
                companyEmail.value = "amines@gmail.com";
                
                // Add a slight flash effect to show it loaded
                [companyName, companyAddress, companyEmail].forEach(el => {
                    el.style.backgroundColor = "#d4edda";
                    setTimeout(() => el.style.backgroundColor = "", 500);
                });
            } else {
                companyName.value = "";
                companyAddress.value = "";
                companyEmail.value = "";
            }
        });
    }

    const directorCount = document.getElementById("directorCount");
    const directorDetailsBlock = document.getElementById("directorDetailsBlock");

    // Toggle the director details block based on number of directors
    if (directorCount && directorDetailsBlock) {
        directorCount.addEventListener("input", function() {
            if (parseInt(this.value) > 0) {
                directorDetailsBlock.style.display = "block";
            } else {
                directorDetailsBlock.style.display = "none";
            }
        });
    }

    // Toggle appointment fields based on purpose
    const purposeRadios = document.querySelectorAll('input[name="purpose1"]');
    const appointmentFields = document.getElementById("appointmentFields");
    
    if (purposeRadios.length > 0 && appointmentFields) {
        purposeRadios.forEach(radio => {
            radio.addEventListener("change", function() {
                if (document.getElementById("purposeAppointment").checked) {
                    appointmentFields.style.display = "block";
                } else {
                    appointmentFields.style.display = "none";
                }
            });
        });
    }

    // Form submission prevention for demo
    const dir12Form = document.getElementById("dir12Form");
    if (dir12Form) {
        dir12Form.addEventListener("submit", function(e) {
            e.preventDefault();
            alert("Form Details Saved Successfully!");
        });
    }
    // Language Translation Logic
    const langEn = document.getElementById("langEn");
    const langHi = document.getElementById("langHi");
    
    const dict = [
        { en: "Company details", hi: "कंपनी विवरण" },
        { en: "Particulars of Director/KMP", hi: "निदेशक/केएमपी का विवरण" },
        { en: "1 (a) * Corporate Identity Number (CIN) of company", hi: "1 (a) * कंपनी की कॉर्पोरेट पहचान संख्या (CIN)" },
        { en: "Enter Company Name To find CIN", hi: "CIN खोजने के लिए कंपनी का नाम दर्ज करें" },
        { en: "(b) *Name of the company", hi: "(b) *कंपनी का नाम" },
        { en: "(c) *Address of the registered office of the company", hi: "(c) *कंपनी के पंजीकृत कार्यालय का पता" },
        { en: "(d) *E-mail ID of the company", hi: "(d) *कंपनी की ई-मेल आईडी" },
        { en: "2 *Number of Managing director or director(s) for which the form is being filed", hi: "2 *प्रबंध निदेशक या निदेशक(कों) की संख्या जिसके लिए फॉर्म दाखिल किया जा रहा है" },
        { en: "3 Details of the Managing Director or Director of the company", hi: "3 प्रबंध निदेशक या कंपनी के निदेशक का विवरण" },
        { en: "(a) Purpose of filing the form", hi: "(a) फॉर्म दाखिल करने का उद्देश्य" },
        { en: "Appointment", hi: "नियुक्ति" },
        { en: "Cessation", hi: "समाप्ति" },
        { en: "Change in designation", hi: "पदनाम में परिवर्तन" },
        { en: "Appointment due to disqualification of all the existing directors", hi: "सभी मौजूदा निदेशकों की अयोग्यता के कारण नियुक्ति" },
        { en: "Appointment by liquidator / IRP/ RP", hi: "लिक्विडेटर / आईआरपी/ आरपी द्वारा नियुक्ति" },
        { en: "(b) Director Identification Number (DIN)", hi: "(b) निदेशक पहचान संख्या (DIN)" },
        { en: "(c) Name", hi: "(c) नाम" },
        { en: "(d) Father's name", hi: "(d) पिता का नाम" },
        { en: "(e) Present residential address", hi: "(e) वर्तमान आवासीय पता" },
        { en: "(f) Nationality", hi: "(f) राष्ट्रीयता" },
        { en: "(g) Date of birth", hi: "(g) जन्म तिथि" },
        { en: "(h) Gender", hi: "(h) लिंग" },
        { en: "(i) E-mail ID of director", hi: "(i) निदेशक की ई-मेल आईडी" },
        { en: "(j) Designation", hi: "(j) पदनाम" },
        { en: "Particulars of KMP", hi: "केएमपी का विवरण" },
        { en: "4 *Number of manager(s), secretary(s), Chief financial Officer or Chief Executive Officer for which the form is being filed", hi: "4 *प्रबंधक(कों), सचिव(ओं), मुख्य वित्तीय अधिकारी या मुख्य कार्यकारी अधिकारी की संख्या जिसके लिए फॉर्म दाखिल किया जा रहा है" },
        { en: "5 Details of manager(s), secretary(s), Chief Financial Officer or Chief Executive Officer of the company", hi: "5 कंपनी के प्रबंधक(कों), सचिव(ओं), मुख्य वित्तीय अधिकारी या मुख्य कार्यकारी अधिकारी का विवरण" },
        { en: "(b) Director Identification Number (DIN), if any", hi: "(b) निदेशक पहचान संख्या (DIN), यदि कोई हो" },
        { en: "(c) Income Tax permanent account number (PAN)", hi: "(c) आयकर स्थायी खाता संख्या (PAN)" },
        { en: "Verify PAN", hi: "पैन सत्यापित करें" },
        { en: "(e)(i) First Name (Either of applicant's First name or Surname shall be mandatory to enter)", hi: "(e)(i) प्रथम नाम (आवेदक का पहला नाम या उपनाम दर्ज करना अनिवार्य होगा)" },
        { en: "(ii) Middle Name", hi: "(ii) मध्य नाम" },
        { en: "(iii) Last Name (Either of applicant's First name or Lastname shall be mandatory to enter)", hi: "(iii) अंतिम नाम (आवेदक का पहला नाम या अंतिम नाम दर्ज करना अनिवार्य होगा)" },
        { en: "(f) Father's name", hi: "(f) पिता का नाम" },
        { en: "(i) First Name (Either of applicant's father's first name or Surname shall be mandatory to enter)", hi: "(i) प्रथम नाम (आवेदक के पिता का पहला नाम या उपनाम दर्ज करना अनिवार्य होगा)" },
        { en: "(iii) Last Name (Either of applicant's father's first name or Surname shall be mandatory to enter)", hi: "(iii) अंतिम नाम (आवेदक के पिता का पहला नाम या उपनाम दर्ज करना अनिवार्य होगा)" },
        { en: "Address Line 1", hi: "पता पंक्ति 1" },
        { en: "Address Line 2", hi: "पता पंक्ति 2" },
        { en: "Country", hi: "देश" },
        { en: "Select Country", hi: "देश चुनें" },
        { en: "Pin Code/Zip code", hi: "पिन कोड/ज़िप कोड" },
        { en: "Area/Locality", hi: "क्षेत्र/मोहल्ला" },
        { en: "City", hi: "शहर" },
        { en: "District", hi: "ज़िला" },
        { en: "State/UT", hi: "राज्य/केंद्र शासित प्रदेश" },
        { en: "Select", hi: "चुनें" },
        { en: "(j) Date of appointment or cessation", hi: "(j) नियुक्ति या समाप्ति की तिथि" },
        { en: "(k) Mobile Number (with Country code)", hi: "(k) मोबाइल नंबर (देश कोड के साथ)" },
        { en: "(l) E-mail ID", hi: "(l) ई-मेल आईडी" },
        { en: "Attachments & Declaration", hi: "संलग्नक और घोषणा" },
        { en: "7 Attachments", hi: "7 संलग्नक" },
        { en: "(d) Optional attachments – if any", hi: "(d) वैकल्पिक संलग्नक – यदि कोई हो" },
        { en: "Director's Consent and Declaration", hi: "निदेशक की सहमति और घोषणा" },
        { en: "Choose File", hi: "फ़ाइल चुनें" },
        { en: "Save", hi: "सहेजें" },
        { en: "Next", hi: "अगला" },
        { en: "Previous", hi: "पिछला" },
        { en: "All fields marked in * are mandatory", hi: "* में चिह्नित सभी फ़ील्ड अनिवार्य हैं" },
        { en: "Form No. DIR-12", hi: "फॉर्म संख्या DIR-12" },
        { en: "Particulars of appointment of directors and the key managerial personnel", hi: "निदेशकों और प्रमुख प्रबंधकीय कर्मियों की नियुक्ति का विवरण" },
        { en: "and the changes among them", hi: "और उनमें परिवर्तन" },
        { en: "[Pursuant to sections 7(1) (c), 168 & 170 (2) of The Companies Act, 2013 and", hi: "[कंपनी अधिनियम, 2013 की धारा 7(1) (सी), 168 और 170 (2) के अनुसरण में" },
        { en: "rule 17 of the Companies (Incorporation) Rules 2014 and 8, 15 & 18 of the", hi: "कंपनी (निगमन) नियम 2014 का नियम 17 और 8, 15 और 18" },
        { en: "Companies (Appointment and Qualification of Directors) Rules, 2014]", hi: "कंपनी (निदेशकों की नियुक्ति और योग्यता) नियम, 2014]" },
        { en: "Attachment & Verification", hi: "संलग्नक और सत्यापन" },
        { en: "Review & Submit", hi: "समीक्षा और सबमिट करें" },
        { en: "Form language", hi: "फॉर्म भाषा" },
        { en: "6 SRN of form INC- 28", hi: "6 फॉर्म INC-28 का SRN" },
        { en: "HOME > MCA Services > Company e-Filing > DIN Related Forms >", hi: "होम > एमसीए सेवाएं > कंपनी ई-फाइलिंग > डीआईएन संबंधित फॉर्म >" },
        { en: "DIR-12 - Appointment of Directors and KMP", hi: "DIR-12 - निदेशकों और केएमपी की नियुक्ति" },
        { en: "English", hi: "अंग्रेज़ी" },
        { en: "Hindi", hi: "हिंदी" },
        { en: "Create an account with us now", hi: "अभी हमारे साथ एक खाता बनाएँ" }
    ];

    function translateForm(lang) {
        const walkDOM = (node) => {
            if (node.nodeType === 3) { 
                let text = node.nodeValue.trim();
                let squashedText = text.replace(/\s+/g, ' ');
                if (text.length > 0) {
                    let match = dict.find(d => d.en === text || d.hi === text || d.en === squashedText || d.hi === squashedText);
                    if (match) {
                        node.nodeValue = node.nodeValue.replace(text, match[lang]);
                    }
                }
            } else if (node.nodeType === 1 && node.nodeName !== 'SCRIPT' && node.nodeName !== 'STYLE') {
                if (node.placeholder) {
                    if (node.placeholder === "Enter Here" && lang === 'hi') node.placeholder = "यहां दर्ज करें";
                    else if (node.placeholder === "यहां दर्ज करें" && lang === 'en') node.placeholder = "Enter Here";
                    
                    if (node.placeholder === "DD/MM/YYYY" && lang === 'hi') node.placeholder = "दिन/माह/वर्ष";
                    else if (node.placeholder === "दिन/माह/वर्ष" && lang === 'en') node.placeholder = "DD/MM/YYYY";
                }
                node.childNodes.forEach(walkDOM);
            }
        };
        walkDOM(document.body);
    }

    const langEn = document.getElementById('langEn');
    const langHi = document.getElementById('langHi');

    if (langEn && langHi) {
        langEn.addEventListener('change', () => {
            localStorage.setItem('formLang', 'en');
            translateForm('en');
        });
        langHi.addEventListener('change', () => {
            localStorage.setItem('formLang', 'hi');
            translateForm('hi');
        });
    }

    // Auto-apply saved language on page load (applies to both pages)
    const savedLang = localStorage.getItem('formLang');
    if (savedLang === 'hi') {
        if (langHi) langHi.checked = true;
        translateForm('hi');
    } else if (savedLang === 'en') {
        if (langEn) langEn.checked = true;
        translateForm('en');
    }

    // Dynamic Login Name logic
    const loginElement = document.getElementById("login");
    const storedName = localStorage.getItem("userName");
    if (loginElement) {
        if (storedName && storedName.trim() !== "") {
            loginElement.innerHTML = `Hello ${storedName} &#9662;`;
        } else {
            loginElement.innerHTML = `Hello User &#9662;`;
        }

        loginElement.addEventListener("click", function(e) {
            const dropdown = this.nextElementSibling;
            if (dropdown) {
                dropdown.style.display = dropdown.style.display === "block" ? "none" : "block";
            }
            e.stopPropagation();
        });
        
        document.addEventListener("click", function(e) {
            const dropdown = loginElement.nextElementSibling;
            if (dropdown) {
                dropdown.style.display = "none";
            }
        });
    }

    const saveFormBtn = document.getElementById("saveFormBtn");
    if (saveFormBtn) {
        saveFormBtn.addEventListener("click", function() {
            const cin = document.getElementById("cinInput") ? document.getElementById("cinInput").value : "U12345DL2024PTC123456";
            const companyName = document.getElementById("companyName") ? document.getElementById("companyName").value : "KHEKRA MINES PRIVATE LIMITED";
            
            const formData = {
                formName: "DIR-12",
                cin: cin,
                companyName: companyName,
                date: new Date().toLocaleDateString(),
                status: "Draft"
            };
            
            let savedForms = JSON.parse(localStorage.getItem("savedForms")) || [];
            savedForms.push(formData);
            localStorage.setItem("savedForms", JSON.stringify(savedForms));
            alert("Form Details Saved to Profile!");
        });
    }

    // Dynamic Declaration fields (attachments.html)
    const declarationName = document.getElementById("declarationName");
    const declarationCompanyName = document.getElementById("declarationCompanyName");
    const declaringPerson = document.getElementById("declaringPerson");
    const authorizationVide = document.getElementById("authorizationVide");
    const dscBox = document.getElementById("dscBox");
    
    if (declarationName) {
        declarationName.value = storedName;
    }
    
    if (declaringPerson) {
        declaringPerson.value = storedName;
    }

    if (dscBox) {
        dscBox.value = storedName ? `Digitally signed by ${storedName}` : "";
    }

    if (declarationCompanyName) {
        // Fetch from API or localStorage. For demo, we use a fixed or stored company name.
        const storedCompany = localStorage.getItem("companyName") || "";
        declarationCompanyName.value = storedCompany;
    }
    
    if (authorizationVide) {
        const storedVide = localStorage.getItem("authorizationVide") || "12345";
        authorizationVide.value = storedVide;
    }

    // File upload logic
    const attachmentFileInput = document.getElementById('attachmentFileInput');
    const attachmentFileName = document.getElementById('attachmentFileName');
    if (attachmentFileInput && attachmentFileName) {
        attachmentFileInput.addEventListener('change', function() {
            if (this.files && this.files.length > 0) {
                attachmentFileName.value = this.files[0].name;
            } else {
                attachmentFileName.value = "";
            }
        });
    }

    // --- Dynamic Form Logic for Section 3 ---
    const purposeRadios = document.querySelectorAll('input[name="purpose1"]');
    const appointmentFields = document.getElementById('appointmentFields');
    const cessationFields = document.getElementById('cessationFields');
    const cessationValidationMsg = document.getElementById('cessationValidationMsg');
    const cessationReason = document.getElementById('cessationReason');

    if (purposeRadios.length > 0) {
        // Initial setup for smooth transition
        if (appointmentFields) appointmentFields.style.transition = 'opacity 0.3s ease-in-out';
        if (cessationFields) cessationFields.style.transition = 'opacity 0.3s ease-in-out';
        
        function clearFields(container) {
            if (!container) return;
            const inputs = container.querySelectorAll('input[type="text"], input[type="number"]');
            inputs.forEach(input => input.value = '');
            
            const selects = container.querySelectorAll('select');
            selects.forEach(select => select.selectedIndex = 0);
            
            const checkboxes = container.querySelectorAll('input[type="checkbox"], input[type="radio"]');
            checkboxes.forEach(checkbox => checkbox.checked = false);
        }

        function showElement(el) {
            if (!el) return;
            el.style.display = 'block';
            setTimeout(() => {
                el.style.opacity = '1';
            }, 10);
        }

        function hideElement(el, shouldClear = true) {
            if (!el) return;
            el.style.opacity = '0';
            setTimeout(() => {
                if (el.style.opacity === '0') {
                    el.style.display = 'none';
                    if (shouldClear) clearFields(el);
                }
            }, 300);
        }

        function validateCessation() {
            const checkedRadio = document.querySelector('input[name="purpose1"]:checked');
            if (checkedRadio && checkedRadio.value === 'Cessation') {
                if (cessationReason && !cessationReason.value) {
                    if (cessationValidationMsg) cessationValidationMsg.style.display = 'block';
                } else {
                    if (cessationValidationMsg) cessationValidationMsg.style.display = 'none';
                }
            } else {
                if (cessationValidationMsg) cessationValidationMsg.style.display = 'none';
            }
        }

        // Function to toggle form sections based on selected value
        function toggleFormSections(value) {
            if (value === 'Appointment') {
                showElement(appointmentFields);
                hideElement(cessationFields);
            } else if (value === 'Cessation') {
                showElement(cessationFields);
                hideElement(appointmentFields);
                validateCessation();
            } else {
                hideElement(appointmentFields);
                hideElement(cessationFields);
            }
            
            if (value !== 'Cessation' && cessationValidationMsg) {
                cessationValidationMsg.style.display = 'none';
            }
        }

        // Add event listeners to all radios
        purposeRadios.forEach(radio => {
            radio.addEventListener('change', function() {
                toggleFormSections(this.value);
            });
        });

        // Add validation listener to reason dropdown
        if (cessationReason) {
            cessationReason.addEventListener('change', validateCessation);
        }

        // Check initial state on page load
        const checkedRadio = document.querySelector('input[name="purpose1"]:checked');
        if (checkedRadio) {
            // Set initial state without animation delays
            if (checkedRadio.value === 'Appointment') {
                if (appointmentFields) { appointmentFields.style.display = 'block'; appointmentFields.style.opacity = '1'; }
                if (cessationFields) { cessationFields.style.display = 'none'; cessationFields.style.opacity = '0'; }
            } else if (checkedRadio.value === 'Cessation') {
                if (appointmentFields) { appointmentFields.style.display = 'none'; appointmentFields.style.opacity = '0'; }
                if (cessationFields) { cessationFields.style.display = 'block'; cessationFields.style.opacity = '1'; }
                validateCessation();
            } else {
                if (appointmentFields) { appointmentFields.style.display = 'none'; appointmentFields.style.opacity = '0'; }
                if (cessationFields) { cessationFields.style.display = 'none'; cessationFields.style.opacity = '0'; }
            }
        } else {
            if (appointmentFields) { appointmentFields.style.display = 'none'; appointmentFields.style.opacity = '0'; }
            if (cessationFields) { cessationFields.style.display = 'none'; cessationFields.style.opacity = '0'; }
        }
    }
});
