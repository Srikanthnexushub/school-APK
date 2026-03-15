// India States and Districts utility
// Provides autocomplete data for state and district fields across the app

export const INDIA_STATES: string[] = [
  'Andhra Pradesh',
  'Arunachal Pradesh',
  'Assam',
  'Bihar',
  'Chhattisgarh',
  'Delhi',
  'Goa',
  'Gujarat',
  'Haryana',
  'Himachal Pradesh',
  'Jharkhand',
  'Karnataka',
  'Kerala',
  'Madhya Pradesh',
  'Maharashtra',
  'Manipur',
  'Meghalaya',
  'Mizoram',
  'Nagaland',
  'Odisha',
  'Punjab',
  'Rajasthan',
  'Sikkim',
  'Tamil Nadu',
  'Telangana',
  'Tripura',
  'Uttar Pradesh',
  'Uttarakhand',
  'West Bengal',
  'Andaman and Nicobar Islands',
  'Chandigarh',
  'Dadra and Nagar Haveli and Daman and Diu',
  'Jammu and Kashmir',
  'Ladakh',
  'Lakshadweep',
  'Puducherry',
];

export const DISTRICTS_BY_STATE: Record<string, string[]> = {
  'Andhra Pradesh': [
    'Visakhapatnam', 'Vijayawada', 'Guntur', 'Nellore', 'Tirupati',
    'Kurnool', 'Kadapa', 'Anantapur', 'East Godavari', 'West Godavari',
  ],
  'Arunachal Pradesh': [
    'Itanagar', 'Tawang', 'East Kameng', 'West Kameng', 'Papum Pare', 'Upper Siang',
  ],
  'Assam': [
    'Guwahati', 'Dibrugarh', 'Jorhat', 'Silchar', 'Tezpur', 'Nagaon',
    'Barpeta', 'Kamrup', 'Cachar',
  ],
  'Bihar': [
    'Patna', 'Gaya', 'Muzaffarpur', 'Bhagalpur', 'Darbhanga', 'Purnia',
    'Nalanda', 'Vaishali', 'Saran', 'Begusarai',
  ],
  'Chhattisgarh': [
    'Raipur', 'Bilaspur', 'Durg', 'Korba', 'Rajnandgaon',
    'Janjgir-Champa', 'Surguja', 'Bastar',
  ],
  'Delhi': [
    'Central Delhi', 'East Delhi', 'New Delhi', 'North Delhi', 'North East Delhi',
    'North West Delhi', 'Shahdara', 'South Delhi', 'South East Delhi',
    'South West Delhi', 'West Delhi',
  ],
  'Goa': [
    'North Goa', 'South Goa',
  ],
  'Gujarat': [
    'Ahmedabad', 'Surat', 'Vadodara', 'Rajkot', 'Bhavnagar',
    'Jamnagar', 'Gandhinagar', 'Anand', 'Mehsana', 'Kutch',
  ],
  'Haryana': [
    'Gurugram', 'Faridabad', 'Ambala', 'Rohtak', 'Hisar',
    'Karnal', 'Sonipat', 'Panipat', 'Yamunanagar', 'Panchkula',
  ],
  'Himachal Pradesh': [
    'Shimla', 'Mandi', 'Kangra', 'Kullu', 'Solan',
    'Hamirpur', 'Sirmaur', 'Bilaspur', 'Chamba', 'Kinnaur',
  ],
  'Jharkhand': [
    'Ranchi', 'Dhanbad', 'Jamshedpur', 'Bokaro', 'Deoghar',
    'Hazaribagh', 'Giridih', 'Ramgarh', 'Dumka',
  ],
  'Karnataka': [
    'Bengaluru Urban', 'Mysuru', 'Hubballi', 'Mangaluru', 'Belagavi',
    'Kalaburagi', 'Tumakuru', 'Davangere', 'Ballari', 'Shivamogga',
  ],
  'Kerala': [
    'Thiruvananthapuram', 'Kochi', 'Kozhikode', 'Thrissur', 'Kollam',
    'Kannur', 'Alappuzha', 'Palakkad', 'Malappuram', 'Kottayam',
  ],
  'Madhya Pradesh': [
    'Bhopal', 'Indore', 'Gwalior', 'Jabalpur', 'Ujjain',
    'Sagar', 'Rewa', 'Satna', 'Dewas', 'Ratlam',
  ],
  'Maharashtra': [
    'Mumbai City', 'Mumbai Suburban', 'Pune', 'Nashik', 'Thane',
    'Aurangabad', 'Nagpur', 'Kolhapur', 'Solapur', 'Amravati',
  ],
  'Manipur': [
    'Imphal East', 'Imphal West', 'Thoubal', 'Bishnupur',
    'Churachandpur', 'Senapati', 'Ukhrul',
  ],
  'Meghalaya': [
    'East Khasi Hills', 'West Khasi Hills', 'Ri-Bhoi',
    'East Jaintia Hills', 'West Jaintia Hills', 'East Garo Hills',
  ],
  'Mizoram': [
    'Aizawl', 'Lunglei', 'Champhai', 'Kolasib', 'Mamit', 'Serchhip', 'Lawngtlai',
  ],
  'Nagaland': [
    'Kohima', 'Dimapur', 'Mokokchung', 'Tuensang', 'Wokha', 'Phek', 'Mon', 'Longleng',
  ],
  'Odisha': [
    'Bhubaneswar', 'Cuttack', 'Rourkela', 'Berhampur', 'Sambalpur',
    'Puri', 'Balasore', 'Bhadrak', 'Bargarh', 'Koraput',
  ],
  'Punjab': [
    'Ludhiana', 'Amritsar', 'Jalandhar', 'Patiala', 'Bathinda',
    'Mohali', 'Hoshiarpur', 'Gurdaspur', 'Ferozepur', 'Fatehgarh Sahib',
  ],
  'Rajasthan': [
    'Jaipur', 'Jodhpur', 'Udaipur', 'Ajmer', 'Kota',
    'Bikaner', 'Alwar', 'Bharatpur', 'Sikar', 'Pali',
  ],
  'Sikkim': [
    'East Sikkim', 'West Sikkim', 'North Sikkim', 'South Sikkim',
  ],
  'Tamil Nadu': [
    'Chennai', 'Coimbatore', 'Madurai', 'Tiruchirappalli', 'Salem',
    'Tirunelveli', 'Vellore', 'Erode', 'Thoothukudi', 'Dindigul',
  ],
  'Telangana': [
    'Hyderabad', 'Warangal', 'Nizamabad', 'Karimnagar', 'Khammam',
    'Nalgonda', 'Medak', 'Adilabad', 'Ranga Reddy', 'Sangareddy',
  ],
  'Tripura': [
    'West Tripura', 'Gomati', 'Sepahijala', 'Khowai',
    'North Tripura', 'Unakoti', 'South Tripura', 'Dhalai',
  ],
  'Uttar Pradesh': [
    'Lucknow', 'Kanpur', 'Varanasi', 'Agra', 'Meerut',
    'Allahabad', 'Ghaziabad', 'Noida', 'Bareilly', 'Moradabad',
  ],
  'Uttarakhand': [
    'Dehradun', 'Haridwar', 'Nainital', 'Udham Singh Nagar', 'Almora',
    'Pauri Garhwal', 'Tehri Garhwal', 'Chamoli', 'Pithoragarh', 'Rudraprayag',
  ],
  'West Bengal': [
    'Kolkata', 'Howrah', 'North 24 Parganas', 'South 24 Parganas', 'Bardhaman',
    'Hooghly', 'Medinipur', 'Nadia', 'Murshidabad', 'Jalpaiguri',
  ],
  'Andaman and Nicobar Islands': [
    'South Andaman', 'North and Middle Andaman', 'Nicobar',
  ],
  'Chandigarh': [
    'Chandigarh',
  ],
  'Dadra and Nagar Haveli and Daman and Diu': [
    'Dadra and Nagar Haveli', 'Daman', 'Diu',
  ],
  'Jammu and Kashmir': [
    'Srinagar', 'Jammu', 'Anantnag', 'Baramulla', 'Budgam',
    'Pulwama', 'Shopian', 'Kupwara', 'Poonch', 'Rajouri',
  ],
  'Ladakh': [
    'Leh', 'Kargil',
  ],
  'Lakshadweep': [
    'Lakshadweep',
  ],
  'Puducherry': [
    'Puducherry', 'Karaikal', 'Mahé', 'Yanam',
  ],
};

/** Return the full list of Indian states / UTs */
export function getStates(): string[] {
  return INDIA_STATES;
}

/** Return districts for a given state, or empty array if state not found */
export function getDistricts(state: string): string[] {
  return DISTRICTS_BY_STATE[state] ?? [];
}

/**
 * Suggest states whose names include the given input (case-insensitive).
 * Returns up to 8 matches.
 */
export function suggestStates(input: string): string[] {
  if (!input) return INDIA_STATES.slice(0, 8);
  const q = input.toLowerCase();
  return INDIA_STATES.filter((s) => s.toLowerCase().includes(q)).slice(0, 8);
}

/**
 * Suggest districts for a given state whose names include the given input (case-insensitive).
 * Returns up to 8 matches.
 */
export function suggestDistricts(state: string, input: string): string[] {
  const districts = DISTRICTS_BY_STATE[state] ?? [];
  if (!input) return districts.slice(0, 8);
  const q = input.toLowerCase();
  return districts.filter((d) => d.toLowerCase().includes(q)).slice(0, 8);
}
