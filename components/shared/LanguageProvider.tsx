'use client';

import { createContext, useContext, useEffect, useMemo, useState } from 'react';

export type AppLanguage = 'en' | 'te' | 'hi' | 'mr';

type I18nContextValue = {
  language: AppLanguage;
  setLanguage: (lang: AppLanguage) => void;
  t: (key: string) => string;
};

const LANG_KEY = 'sarathi_lang';

const translations: Record<AppLanguage, Record<string, string>> = {
  en: {
    'lang.label': 'Language',
    'nav.home': 'Home',
    'nav.driver': 'Driver Portal',
    'nav.emergency': 'Emergency',
    'nav.hospital': 'Hospital',
    'nav.police': 'Police',
    'nav.driverLogin': 'Driver Login',
    'nav.sos': 'Emergency SOS',
    'nav.hospitalLogin': 'Hospital Login',
    'nav.policeLogin': 'Police Login',
    'nav.logout': 'Logout',
    'nav.loggedIn': 'Logged in as',
    'home.saveLives': 'Save Lives',
    'home.withPrecision': 'With Precision',
    'home.subtitle': 'AI-powered emergency navigation for ambulances, police, and fire services.',
    'home.choosePortal': 'Choose Your Portal',
    'home.driverPortal': 'Driver Portal',
    'home.sosPortal': 'Emergency SOS',
    'home.adminPortal': 'Admin / Hospital',
    'home.loginDriver': 'Login as Driver',
    'home.sendAlert': 'Send Emergency Alert',
    'home.adminDashboard': 'Admin Dashboard',
    'sos.title': 'Emergency SOS',
    'sos.subtitle': 'Phone number + one tap. We handle the rest.',
    'sos.phone': 'Phone Number',
    'sos.send': 'SEND SOS',
    'sos.sending': 'Sending SOS...',
    'sos.trackTitle': 'Track Existing SOS',
    'sos.track': 'Track SOS',
    'sos.tracking': 'Tracking...',
    'sos.success': 'SOS sent successfully',
    'common.backHome': 'Back to Home',
    'driverLogin.title': 'Driver Portal',
    'driverLogin.email': 'Email Address',
    'driverLogin.password': 'Password',
    'driverLogin.remember': 'Remember me',
    'driverLogin.forgot': 'Forgot Password?',
    'driverLogin.signIn': 'Sign In',
    'driverLogin.signing': 'Signing In...',
    'driverLogin.otp': 'Sign In with OTP',
    'driverLogin.noAccount': "Don't have an account?",
    'driverLogin.register': 'Register as Driver',
    'hospitalLogin.title': 'Hospital Login',
    'hospitalLogin.subtitle': 'Live emergency case intake dashboard',
    'hospitalLogin.hospital': 'Hospital',
    'hospitalLogin.search': 'Search hospital name, area, phone...',
    'hospitalLogin.phone': 'Hospital Phone',
    'hospitalLogin.login': 'Login to Hospital Dashboard',
    'policeLogin.title': 'Police Login',
    'policeLogin.subtitle': 'Green corridor alert dashboard',
    'policeLogin.station': 'Police Station',
    'policeLogin.search': 'Search station/jurisdiction/zone...',
    'policeLogin.phone': 'Station Phone',
    'policeLogin.login': 'Login to Police Dashboard',
  },
  te: {
    'lang.label': 'భాష',
    'nav.home': 'హోమ్',
    'nav.driver': 'డ్రైవర్ పోర్టల్',
    'nav.emergency': 'అత్యవసరం',
    'nav.hospital': 'హాస్పిటల్',
    'nav.police': 'పోలీస్',
    'nav.driverLogin': 'డ్రైవర్ లాగిన్',
    'nav.sos': 'ఎమర్జెన్సీ SOS',
    'nav.hospitalLogin': 'హాస్పిటల్ లాగిన్',
    'nav.policeLogin': 'పోలీస్ లాగిన్',
    'nav.logout': 'లాగౌట్',
    'nav.loggedIn': 'లాగిన్ అయిన వారు',
    'home.saveLives': 'ప్రాణాలను కాపాడండి',
    'home.withPrecision': 'ఖచ్చితత్వంతో',
    'home.subtitle': 'అంబులెన్స్, పోలీస్ మరియు ఫైర్ సేవలకు AI ఆధారిత నావిగేషన్.',
    'home.choosePortal': 'మీ పోర్టల్ ఎంచుకోండి',
    'home.driverPortal': 'డ్రైవర్ పోర్టల్',
    'home.sosPortal': 'ఎమర్జెన్సీ SOS',
    'home.adminPortal': 'అడ్మిన్ / హాస్పిటల్',
    'home.loginDriver': 'డ్రైవర్‌గా లాగిన్',
    'home.sendAlert': 'ఎమర్జెన్సీ అలర్ట్ పంపండి',
    'home.adminDashboard': 'అడ్మిన్ డ్యాష్‌బోర్డ్',
    'sos.title': 'ఎమర్జెన్సీ SOS',
    'sos.subtitle': 'ఫోన్ నెంబర్ + ఒక ట్యాప్. మిగతాదంతా మేమే చూసుకుంటాము.',
    'sos.phone': 'ఫోన్ నెంబర్',
    'sos.send': 'SOS పంపండి',
    'sos.sending': 'SOS పంపుతోంది...',
    'sos.trackTitle': 'పాత SOS ట్రాక్ చేయండి',
    'sos.track': 'SOS ట్రాక్ చేయండి',
    'sos.tracking': 'ట్రాక్ అవుతోంది...',
    'sos.success': 'SOS విజయవంతంగా పంపబడింది',
    'common.backHome': 'హోమ్ కి వెనక్కి',
    'driverLogin.title': 'డ్రైవర్ పోర్టల్',
    'driverLogin.email': 'ఈమెయిల్ అడ్రెస్',
    'driverLogin.password': 'పాస్‌వర్డ్',
    'driverLogin.remember': 'నన్ను గుర్తుంచుకోండి',
    'driverLogin.forgot': 'పాస్‌వర్డ్ మర్చిపోయారా?',
    'driverLogin.signIn': 'సైన్ ఇన్',
    'driverLogin.signing': 'సైన్ ఇన్ అవుతోంది...',
    'driverLogin.otp': 'OTP తో సైన్ ఇన్',
    'driverLogin.noAccount': 'ఖాతా లేదా?',
    'driverLogin.register': 'డ్రైవర్‌గా రిజిస్టర్',
    'hospitalLogin.title': 'హాస్పిటల్ లాగిన్',
    'hospitalLogin.subtitle': 'లైవ్ ఎమర్జెన్సీ కేస్ డ్యాష్‌బోర్డ్',
    'hospitalLogin.hospital': 'హాస్పిటల్',
    'hospitalLogin.search': 'హాస్పిటల్ పేరు, ప్రాంతం, ఫోన్ వెతకండి...',
    'hospitalLogin.phone': 'హాస్పిటల్ ఫోన్',
    'hospitalLogin.login': 'హాస్పిటల్ డ్యాష్‌బోర్డ్ లోకి లాగిన్',
    'policeLogin.title': 'పోలీస్ లాగిన్',
    'policeLogin.subtitle': 'గ్రీన్ కారిడార్ అలర్ట్ డ్యాష్‌బోర్డ్',
    'policeLogin.station': 'పోలీస్ స్టేషన్',
    'policeLogin.search': 'స్టేషన్/ప్రాంతం/జోన్ వెతకండి...',
    'policeLogin.phone': 'స్టేషన్ ఫోన్',
    'policeLogin.login': 'పోలీస్ డ్యాష్‌బోర్డ్ లోకి లాగిన్',
  },
  hi: {
    'lang.label': 'भाषा',
    'nav.home': 'होम',
    'nav.driver': 'ड्राइवर पोर्टल',
    'nav.emergency': 'इमरजेंसी',
    'nav.hospital': 'अस्पताल',
    'nav.police': 'पुलिस',
    'nav.driverLogin': 'ड्राइवर लॉगिन',
    'nav.sos': 'इमरजेंसी SOS',
    'nav.hospitalLogin': 'अस्पताल लॉगिन',
    'nav.policeLogin': 'पुलिस लॉगिन',
    'nav.logout': 'लॉगआउट',
    'nav.loggedIn': 'लॉगिन यूज़र',
    'home.saveLives': 'जान बचाइए',
    'home.withPrecision': 'सटीकता के साथ',
    'home.subtitle': 'एम्बुलेंस, पुलिस और फायर सेवाओं के लिए AI आधारित नेविगेशन।',
    'home.choosePortal': 'अपना पोर्टल चुनें',
    'home.driverPortal': 'ड्राइवर पोर्टल',
    'home.sosPortal': 'इमरजेंसी SOS',
    'home.adminPortal': 'एडमिन / अस्पताल',
    'home.loginDriver': 'ड्राइवर लॉगिन',
    'home.sendAlert': 'इमरजेंसी अलर्ट भेजें',
    'home.adminDashboard': 'एडमिन डैशबोर्ड',
    'sos.title': 'इमरजेंसी SOS',
    'sos.subtitle': 'फोन नंबर + एक टैप। बाकी हम संभालेंगे।',
    'sos.phone': 'फोन नंबर',
    'sos.send': 'SOS भेजें',
    'sos.sending': 'SOS भेजा जा रहा है...',
    'sos.trackTitle': 'मौजूदा SOS ट्रैक करें',
    'sos.track': 'SOS ट्रैक करें',
    'sos.tracking': 'ट्रैक हो रहा है...',
    'sos.success': 'SOS सफलतापूर्वक भेजा गया',
    'common.backHome': 'होम पर वापस जाएँ',
    'driverLogin.title': 'ड्राइवर पोर्टल',
    'driverLogin.email': 'ईमेल पता',
    'driverLogin.password': 'पासवर्ड',
    'driverLogin.remember': 'मुझे याद रखें',
    'driverLogin.forgot': 'पासवर्ड भूल गए?',
    'driverLogin.signIn': 'साइन इन',
    'driverLogin.signing': 'साइन इन हो रहा है...',
    'driverLogin.otp': 'OTP से साइन इन',
    'driverLogin.noAccount': 'अकाउंट नहीं है?',
    'driverLogin.register': 'ड्राइवर रजिस्टर करें',
    'hospitalLogin.title': 'अस्पताल लॉगिन',
    'hospitalLogin.subtitle': 'लाइव इमरजेंसी केस इनटेक डैशबोर्ड',
    'hospitalLogin.hospital': 'अस्पताल',
    'hospitalLogin.search': 'अस्पताल नाम, एरिया, फोन खोजें...',
    'hospitalLogin.phone': 'अस्पताल फोन',
    'hospitalLogin.login': 'अस्पताल डैशबोर्ड लॉगिन',
    'policeLogin.title': 'पुलिस लॉगिन',
    'policeLogin.subtitle': 'ग्रीन कॉरिडोर अलर्ट डैशबोर्ड',
    'policeLogin.station': 'पुलिस स्टेशन',
    'policeLogin.search': 'स्टेशन/जुरिस्डिक्शन/ज़ोन खोजें...',
    'policeLogin.phone': 'स्टेशन फोन',
    'policeLogin.login': 'पुलिस डैशबोर्ड लॉगिन',
  },
  mr: {
    'lang.label': 'भाषा',
    'nav.home': 'मुख्यपृष्ठ',
    'nav.driver': 'ड्रायव्हर पोर्टल',
    'nav.emergency': 'आपत्कालीन',
    'nav.hospital': 'रुग्णालय',
    'nav.police': 'पोलीस',
    'nav.driverLogin': 'ड्रायव्हर लॉगिन',
    'nav.sos': 'आपत्कालीन SOS',
    'nav.hospitalLogin': 'रुग्णालय लॉगिन',
    'nav.policeLogin': 'पोलीस लॉगिन',
    'nav.logout': 'लॉगआउट',
    'nav.loggedIn': 'लॉगिन वापरकर्ता',
    'home.saveLives': 'जीव वाचा',
    'home.withPrecision': 'अचूकतेसह',
    'home.subtitle': 'अॅम्बुलन्स, पोलीस आणि फायर सेवांसाठी AI आधारित नेव्हिगेशन.',
    'home.choosePortal': 'तुमचे पोर्टल निवडा',
    'home.driverPortal': 'ड्रायव्हर पोर्टल',
    'home.sosPortal': 'आपत्कालीन SOS',
    'home.adminPortal': 'अॅडमिन / रुग्णालय',
    'home.loginDriver': 'ड्रायव्हर लॉगिन',
    'home.sendAlert': 'आपत्कालीन अलर्ट पाठवा',
    'home.adminDashboard': 'अॅडमिन डॅशबोर्ड',
    'sos.title': 'आपत्कालीन SOS',
    'sos.subtitle': 'फोन नंबर + एक टॅप. उरलेले आम्ही पाहतो.',
    'sos.phone': 'फोन नंबर',
    'sos.send': 'SOS पाठवा',
    'sos.sending': 'SOS पाठवले जात आहे...',
    'sos.trackTitle': 'अस्तित्वात असलेले SOS ट्रॅक करा',
    'sos.track': 'SOS ट्रॅक करा',
    'sos.tracking': 'ट्रॅक होत आहे...',
    'sos.success': 'SOS यशस्वीरित्या पाठवले',
    'common.backHome': 'मुख्यपृष्ठावर जा',
    'driverLogin.title': 'ड्रायव्हर पोर्टल',
    'driverLogin.email': 'ईमेल पत्ता',
    'driverLogin.password': 'पासवर्ड',
    'driverLogin.remember': 'मला लक्षात ठेवा',
    'driverLogin.forgot': 'पासवर्ड विसरलात?',
    'driverLogin.signIn': 'साइन इन',
    'driverLogin.signing': 'साइन इन होत आहे...',
    'driverLogin.otp': 'OTP ने साइन इन',
    'driverLogin.noAccount': 'खाते नाही?',
    'driverLogin.register': 'ड्रायव्हर नोंदणी करा',
    'hospitalLogin.title': 'रुग्णालय लॉगिन',
    'hospitalLogin.subtitle': 'लाइव्ह आपत्कालीन केस डॅशबोर्ड',
    'hospitalLogin.hospital': 'रुग्णालय',
    'hospitalLogin.search': 'रुग्णालय नाव, भाग, फोन शोधा...',
    'hospitalLogin.phone': 'रुग्णालय फोन',
    'hospitalLogin.login': 'रुग्णालय डॅशबोर्ड लॉगिन',
    'policeLogin.title': 'पोलीस लॉगिन',
    'policeLogin.subtitle': 'ग्रीन कॉरिडोर अलर्ट डॅशबोर्ड',
    'policeLogin.station': 'पोलीस स्टेशन',
    'policeLogin.search': 'स्टेशन/जुरिस्डिक्शन/झोन शोधा...',
    'policeLogin.phone': 'स्टेशन फोन',
    'policeLogin.login': 'पोलीस डॅशबोर्ड लॉगिन',
  },
};

const I18nContext = createContext<I18nContextValue | null>(null);

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<AppLanguage>('en');

  useEffect(() => {
    const saved = localStorage.getItem(LANG_KEY) as AppLanguage | null;
    if (saved && translations[saved]) {
      setLanguageState(saved);
      return;
    }

    const browserLang = (navigator.language || 'en').toLowerCase();
    if (browserLang.startsWith('te')) {
      setLanguageState('te');
      return;
    }
    if (browserLang.startsWith('hi')) {
      setLanguageState('hi');
      return;
    }
    if (browserLang.startsWith('mr')) {
      setLanguageState('mr');
      return;
    }
  }, []);

  const setLanguage = (lang: AppLanguage) => {
    setLanguageState(lang);
    localStorage.setItem(LANG_KEY, lang);
  };

  const t = useMemo(
    () => (key: string) => translations[language]?.[key] ?? translations.en[key] ?? key,
    [language]
  );

  return <I18nContext.Provider value={{ language, setLanguage, t }}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) {
    throw new Error('useI18n must be used inside LanguageProvider');
  }
  return ctx;
}
