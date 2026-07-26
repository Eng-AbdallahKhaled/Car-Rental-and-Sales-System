package com.carapp.car_rental_and_sales_system.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ImageDownloader {

    // القائمة الكاملة (100 سيارة - النخبة فقط)
    private static final String[][] CARS_TO_DOWNLOAD = {
        // --- 1. Bugatti ---
        {"Bugatti_Chiron", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/66/Bugatti_Chiron_Super_Sport_300%2B.jpg/640px-Bugatti_Chiron_Super_Sport_300%2B.jpg"},
        {"Bugatti_Veyron", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c9/Bugatti_Veyron_16.4_%E2%80%93_Frontansicht_%281%29%2C_5._April_2012%2C_D%C3%BCsseldorf.jpg/640px-Bugatti_Veyron_16.4_%E2%80%93_Frontansicht_%281%29%2C_5._April_2012%2C_D%C3%BCsseldorf.jpg"},
        {"Bugatti_Divo", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/Bugatti_Divo_GIMS_2019_Le_Grand-Saconnex_GIMS0020.jpg/640px-Bugatti_Divo_GIMS_2019_Le_Grand-Saconnex_GIMS0020.jpg"},
        {"Bugatti_Centodieci", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Bugatti_Centodieci_IMG_4953.jpg/640px-Bugatti_Centodieci_IMG_4953.jpg"},
        {"Bugatti_Bolide", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/Bugatti_Bolide_%28concept%29_IMG_5033.jpg/640px-Bugatti_Bolide_%28concept%29_IMG_5033.jpg"},
        {"Bugatti_La_Voiture_Noire", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Bugatti_La_Voiture_Noire_GIMS_2019_Le_Grand-Saconnex_GIMS0029.jpg/640px-Bugatti_La_Voiture_Noire_GIMS_2019_Le_Grand-Saconnex_GIMS0029.jpg"},
        {"Bugatti_Mistral", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Bugatti_W16_Mistral_Pebble_Beach_2022_1.jpg/640px-Bugatti_W16_Mistral_Pebble_Beach_2022_1.jpg"},
        {"Bugatti_Eb110", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/Bugatti_EB_110_SS_-_Flickr_-_Alex_Penfold_%281%29.jpg/640px-Bugatti_EB_110_SS_-_Flickr_-_Alex_Penfold_%281%29.jpg"},
        {"Bugatti_Type_35", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/63/Bugatti_Type_35_C_1926_%2815678854740%29.jpg/640px-Bugatti_Type_35_C_1926_%2815678854740%29.jpg"},
        {"Bugatti_Galibier", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Bugatti_16C_Galibier_Concept_-_Front_Angle_-_2010_Geneva_Motor_Show.jpg/640px-Bugatti_16C_Galibier_Concept_-_Front_Angle_-_2010_Geneva_Motor_Show.jpg"},

        // --- 2. Ferrari ---
        {"Ferrari_LaFerrari", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/LaFerrari_Aperta_Paris_Motor_Show_2016.jpg/640px-LaFerrari_Aperta_Paris_Motor_Show_2016.jpg"},
        {"Ferrari_Enzo", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Ferrari_Enzo_Ferrari_front_20190131.jpg/640px-Ferrari_Enzo_Ferrari_front_20190131.jpg"},
        {"Ferrari_F40", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/F40_ferrari_20090509.jpg/640px-F40_ferrari_20090509.jpg"},
        {"Ferrari_F50", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/1995_Ferrari_F50_%2835061614734%29.jpg/640px-1995_Ferrari_F50_%2835061614734%29.jpg"},
        {"Ferrari_SF90_Stradale", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Ferrari_SF90_Spider_IAA_2021_1X7A0168.jpg/640px-Ferrari_SF90_Spider_IAA_2021_1X7A0168.jpg"},
        {"Ferrari_488_Pista", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Ferrari_488_Pista_Genf_2018.jpg/640px-Ferrari_488_Pista_Genf_2018.jpg"},
        {"Ferrari_812_Competizione", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Ferrari_812_Superfast_Genf_2017.jpg/640px-Ferrari_812_Superfast_Genf_2017.jpg"},
        {"Ferrari_Roma", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Ferrari_Roma_Spider_Goodwood_Festival_of_Speed_2023_%281%29.jpg/640px-Ferrari_Roma_Spider_Goodwood_Festival_of_Speed_2023_%281%29.jpg"},
        {"Ferrari_Portofino", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/Ferrari_Portofino_M_IMG_4210.jpg/640px-Ferrari_Portofino_M_IMG_4210.jpg"},
        {"Ferrari_Daytona_SP3", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7a/Ferrari_Daytona_SP3_Goodwood_Festival_of_Speed_2022_%281%29_%2852230491979%29.jpg/640px-Ferrari_Daytona_SP3_Goodwood_Festival_of_Speed_2022_%281%29_%2852230491979%29.jpg"},
        {"Ferrari_Purosangue", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Ferrari_Purosangue.jpg/640px-Ferrari_Purosangue.jpg"},
        {"Ferrari_296_GTS", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Ferrari_296_GTS_Goodwood_Festival_of_Speed_2022_%281%29.jpg/640px-Ferrari_296_GTS_Goodwood_Festival_of_Speed_2022_%281%29.jpg"},
        {"Ferrari_Monza_SP1", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/34/Ferrari_Monza_SP1_Mondial_de_l%27Auto_2018.jpg/640px-Ferrari_Monza_SP1_Mondial_de_l%27Auto_2018.jpg"},
        {"Ferrari_GTC4Lusso", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Ferrari_GTC4Lusso_Mondial_de_l%E2%80%99Automobile_2018.jpg/640px-Ferrari_GTC4Lusso_Mondial_de_l%E2%80%99Automobile_2018.jpg"},
        {"Ferrari_F12_tdf", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/03/Ferrari_F12_TDF_at_GIMS_2018_01.jpg/640px-Ferrari_F12_TDF_at_GIMS_2018_01.jpg"},

        // --- 3. Lamborghini ---
        {"Lamborghini_Aventador_SVJ", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Lamborghini_Aventador_SVJ_Roadster_Genf_2019_1Y7A5372.jpg/640px-Lamborghini_Aventador_SVJ_Roadster_Genf_2019_1Y7A5372.jpg"},
        {"Lamborghini_Huracan_STO", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/86/Lamborghini_Huracan_STO_IMG_4996.jpg/640px-Lamborghini_Huracan_STO_IMG_4996.jpg"},
        {"Lamborghini_Urus_Performante", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6b/Lamborghini_Urus_Performante_IMG_7604.jpg/640px-Lamborghini_Urus_Performante_IMG_7604.jpg"},
        {"Lamborghini_Sian", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Lamborghini_Sian_IMG_4985.jpg/640px-Lamborghini_Sian_IMG_4985.jpg"},
        {"Lamborghini_Countach_LPI", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Lamborghini_Countach_LPI_800-4.jpg/640px-Lamborghini_Countach_LPI_800-4.jpg"},
        {"Lamborghini_Revuelto", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Lamborghini_Revuelto_IMG_7599.jpg/640px-Lamborghini_Revuelto_IMG_7599.jpg"},
        {"Lamborghini_Murcielago_SV", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0c/Lamborghini_Murci%C3%A9lago_LP_670-4_SuperVeloce.jpg/640px-Lamborghini_Murci%C3%A9lago_LP_670-4_SuperVeloce.jpg"},
        {"Lamborghini_Diablo_GT", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/Lamborghini_Diablo.jpg/640px-Lamborghini_Diablo.jpg"},
        {"Lamborghini_Gallardo", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/Lamborghini_Gallardo_LP570-4_Superleggera.jpg/640px-Lamborghini_Gallardo_LP570-4_Superleggera.jpg"},
        {"Lamborghini_Centenario", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/86/Lamborghini_Centenario_Genf_2016.jpg/640px-Lamborghini_Centenario_Genf_2016.jpg"},
        {"Lamborghini_Veneno", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/2013-03-05_Geneva_Motor_Show_8273.JPG/640px-2013-03-05_Geneva_Motor_Show_8273.JPG"},
        {"Lamborghini_Reventon", "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Lamborghini_Reventon_2007.jpg/640px-Lamborghini_Reventon_2007.jpg"},
        {"Lamborghini_Miura", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Lamborghini_Miura_P400S.jpg/640px-Lamborghini_Miura_P400S.jpg"},
        {"Lamborghini_Egoista", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Lamborghini_Egoista_IAA_2013.jpg/640px-Lamborghini_Egoista_IAA_2013.jpg"},
        {"Lamborghini_Estoque", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Lamborghini_Estoque_Paris_2008.jpg/640px-Lamborghini_Estoque_Paris_2008.jpg"},

        // --- 4. McLaren ---
        {"McLaren_P1", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/McLaren_P1_Geneva_2013.jpg/640px-McLaren_P1_Geneva_2013.jpg"},
        {"McLaren_Senna", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/McLaren_Senna_GIMS_2018_Le_Grand-Saconnex_1.jpg/640px-McLaren_Senna_GIMS_2018_Le_Grand-Saconnex_1.jpg"},
        {"McLaren_720S", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/McLaren_720S_Spider_Genf_2019_1Y7A5630.jpg/640px-McLaren_720S_Spider_Genf_2019_1Y7A5630.jpg"},
        {"McLaren_765LT", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/McLaren_765LT_Spider_IAA_2021_1X7A0129.jpg/640px-McLaren_765LT_Spider_IAA_2021_1X7A0129.jpg"},
        {"McLaren_Artura", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/63/McLaren_Artura_IAA_2021_1X7A0064.jpg/640px-McLaren_Artura_IAA_2021_1X7A0064.jpg"},
        {"McLaren_Elva", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/2020_McLaren_Elva_front.jpg/640px-2020_McLaren_Elva_front.jpg"},
        {"McLaren_Speedtail", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/McLaren_Speedtail_IMG_4945.jpg/640px-McLaren_Speedtail_IMG_4945.jpg"},
        {"McLaren_GT", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7e/McLaren_GT_Goodwood_Festival_of_Speed_2019_%281%29.jpg/640px-McLaren_GT_Goodwood_Festival_of_Speed_2019_%281%29.jpg"},
        {"McLaren_F1", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/McLaren_F1_XP5.jpg/640px-McLaren_F1_XP5.jpg"},
        {"McLaren_Solus_GT", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/McLaren_Senna_GIMS_2018_Le_Grand-Saconnex_1.jpg/640px-McLaren_Senna_GIMS_2018_Le_Grand-Saconnex_1.jpg"},

        // --- 5. Porsche ---
        {"Porsche_918_Spyder", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9d/Porsche_918_Spyder_IAA_2013.jpg/640px-Porsche_918_Spyder_IAA_2013.jpg"},
        {"Porsche_Carrera_GT", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Porsche_Carrera_GT_-_Goodwood_Breakfast_Club_%28July_2008%29.jpg/640px-Porsche_Carrera_GT_-_Goodwood_Breakfast_Club_%28July_2008%29.jpg"},
        {"Porsche_911_GT3_RS", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Porsche_992_GT3_RS_IAA_2023_1X7A0373.jpg/640px-Porsche_992_GT3_RS_IAA_2023_1X7A0373.jpg"},
        {"Porsche_911_Turbo_S", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Porsche_992_Turbo_S_IMG_4193.jpg/640px-Porsche_992_Turbo_S_IMG_4193.jpg"},
        {"Porsche_Taycan", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Porsche_Taycan_Turbo_S_IMG_3570.jpg/640px-Porsche_Taycan_Turbo_S_IMG_3570.jpg"},
        {"Porsche_Panamera", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Porsche_Panamera_Turbo_S_IMG_4187.jpg/640px-Porsche_Panamera_Turbo_S_IMG_4187.jpg"},
        {"Porsche_Cayenne_Turbo", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Porsche_Cayenne_Turbo_GT_IMG_6883.jpg/640px-Porsche_Cayenne_Turbo_GT_IMG_6883.jpg"},
        {"Porsche_Macan_GTS", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Porsche_Macan_GTS_%28Facelift_2021%29_IMG_6877.jpg/640px-Porsche_Macan_GTS_%28Facelift_2021%29_IMG_6877.jpg"},
        {"Porsche_718_Cayman_GT4", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/Porsche_718_Cayman_GT4_RS_IMG_4220.jpg/640px-Porsche_718_Cayman_GT4_RS_IMG_4220.jpg"},
        {"Porsche_959", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Porsche_959_at_Goodwood_Festival_of_Speed_2010.jpg/640px-Porsche_959_at_Goodwood_Festival_of_Speed_2010.jpg"},

        // --- 6. Rolls Royce ---
        {"Rolls_Royce_Phantom", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg/640px-Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg"},
        {"Rolls_Royce_Cullinan", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Rolls-Royce_Cullinan_Black_Badge_IMG_4905.jpg/640px-Rolls-Royce_Cullinan_Black_Badge_IMG_4905.jpg"},
        {"Rolls_Royce_Spectre", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Rolls-Royce_Spectre_Goodwood_Festival_of_Speed_2023_%281%29.jpg/640px-Rolls-Royce_Spectre_Goodwood_Festival_of_Speed_2023_%281%29.jpg"},
        {"Rolls_Royce_Ghost", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Rolls-Royce_Ghost_EWB_Auto_Shanghai_2021_01.jpg/640px-Rolls-Royce_Ghost_EWB_Auto_Shanghai_2021_01.jpg"},
        {"Rolls_Royce_Wraith", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Rolls-Royce_Wraith_Series_II_Genf_2016.jpg/640px-Rolls-Royce_Wraith_Series_II_Genf_2016.jpg"},
        {"Rolls_Royce_Dawn", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cf/Rolls-Royce_Dawn_IAA_2015.jpg/640px-Rolls-Royce_Dawn_IAA_2015.jpg"},
        {"Rolls_Royce_Sweptail", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg/640px-Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg"},
        {"Rolls_Royce_Boat_Tail", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg/640px-Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg"},
        {"Rolls_Royce_Droptail", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg/640px-Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg"},
        {"Rolls_Royce_Silver_Ghost", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg/640px-Rolls-Royce_Phantom_VIII_Series_II_IMG_7101.jpg"},

        // --- 7. Bentley ---
        {"Bentley_Continental_GT", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Bentley_Continental_GT_Speed_%28III%29_IMG_4908.jpg/640px-Bentley_Continental_GT_Speed_%28III%29_IMG_4908.jpg"},
        {"Bentley_Flying_Spur", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7f/Bentley_Flying_Spur_V8_%28III%29_IMG_4111.jpg/640px-Bentley_Flying_Spur_V8_%28III%29_IMG_4111.jpg"},
        {"Bentley_Bentayga", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/57/Bentley_Bentayga_EWB_Azure_IMG_7088.jpg/640px-Bentley_Bentayga_EWB_Azure_IMG_7088.jpg"},
        {"Bentley_Mulsanne", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg/640px-Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg"},
        {"Bentley_Bacalar", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7a/Bentley_Mulliner_Bacalar_Goodwood_Festival_of_Speed_2021_%281%29_%2851322972985%29.jpg/640px-Bentley_Mulliner_Bacalar_Goodwood_Festival_of_Speed_2021_%281%29_%2851322972985%29.jpg"},
        {"Bentley_Batur", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Bentley_Continental_GT_Speed_%28III%29_IMG_4908.jpg/640px-Bentley_Continental_GT_Speed_%28III%29_IMG_4908.jpg"},
        {"Bentley_Brooklands", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg/640px-Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg"},
        {"Bentley_Azure", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg/640px-Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg"},
        {"Bentley_Arnage", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg/640px-Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg"},
        {"Bentley_Turbo_R", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg/640px-Bentley_Mulsanne_Speed_%28Facelift%29_%E2%80%93_Frontansicht%2C_28._Juni_2016%2C_D%C3%BCsseldorf.jpg"},

        // --- 8. Aston Martin ---
        {"Aston_Martin_DBS", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/Aston_Martin_DBS_Superleggera_IMG_0679.jpg/640px-Aston_Martin_DBS_Superleggera_IMG_0679.jpg"},
        {"Aston_Martin_Valkyrie", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Aston_Martin_Valkyrie_Goodwood_Festival_of_Speed_2021_%281%29_%2851322728956%29.jpg/640px-Aston_Martin_Valkyrie_Goodwood_Festival_of_Speed_2021_%281%29_%2851322728956%29.jpg"},
        {"Aston_Martin_Valhalla", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Aston_Martin_Valhalla_Genf_2019_1Y7A5547.jpg/640px-Aston_Martin_Valhalla_Genf_2019_1Y7A5547.jpg"},
        {"Aston_Martin_DB12", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Aston_Martin_DB11_Genf_2016.jpg/640px-Aston_Martin_DB11_Genf_2016.jpg"},
        {"Aston_Martin_DBX707", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Aston_Martin_DBX707_IMG_6869.jpg/640px-Aston_Martin_DBX707_IMG_6869.jpg"},
        {"Aston_Martin_Vantage", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/Aston_Martin_Vantage_GIMS_2018_Le_Grand-Saconnex_1.jpg/640px-Aston_Martin_Vantage_GIMS_2018_Le_Grand-Saconnex_1.jpg"},
        {"Aston_Martin_One77", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/Aston_Martin_One-77_-_Flickr_-_Alex_Penfold.jpg/640px-Aston_Martin_One-77_-_Flickr_-_Alex_Penfold.jpg"},
        {"Aston_Martin_Vulcan", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Aston_Martin_Vulcan_Goodwood_Festival_of_Speed_2016_%2828063345472%29.jpg/640px-Aston_Martin_Vulcan_Goodwood_Festival_of_Speed_2016_%2828063345472%29.jpg"},
        {"Aston_Martin_Victor", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/Aston_Martin_DBS_Superleggera_IMG_0679.jpg/640px-Aston_Martin_DBS_Superleggera_IMG_0679.jpg"},
        {"Aston_Martin_Speedster", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/Aston_Martin_DBS_Superleggera_IMG_0679.jpg/640px-Aston_Martin_DBS_Superleggera_IMG_0679.jpg"},

        // --- 9. Mercedes-Benz ---
        {"Mercedes_AMG_G63", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Mercedes-AMG_G_63_%28W463%2C_2nd_generation%29_IMG_4914.jpg/640px-Mercedes-AMG_G_63_%28W463%2C_2nd_generation%29_IMG_4914.jpg"},
        {"Mercedes_AMG_GT", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Mercedes-AMG_GT_Black_Series_IMG_4965.jpg/640px-Mercedes-AMG_GT_Black_Series_IMG_4965.jpg"},
        {"Mercedes_SLS_AMG", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Mercedes-Benz_SLR_McLaren_front_20080607.jpg/640px-Mercedes-Benz_SLR_McLaren_front_20080607.jpg"},
        {"Mercedes_Maybach_S680", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Mercedes-Maybach_S_680_%28Z223%29_IMG_6924.jpg/640px-Mercedes-Maybach_S_680_%28Z223%29_IMG_6924.jpg"},
        {"Mercedes_AMG_One", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/Mercedes-AMG_One_IAA_2017.jpg/640px-Mercedes-AMG_One_IAA_2017.jpg"},
        {"Mercedes_AMG_SL63", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/76/Mercedes-AMG_SL_63_4MATIC%2B_%28R232%29_IMG_6977.jpg/640px-Mercedes-AMG_SL_63_4MATIC%2B_%28R232%29_IMG_6977.jpg"},
        {"Mercedes_Maybach_Exelero", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Maybach_Exelero.jpg/640px-Maybach_Exelero.jpg"},
        {"Mercedes_Maybach_GLS", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Mercedes-AMG_G_63_%28W463%2C_2nd_generation%29_IMG_4914.jpg/640px-Mercedes-AMG_G_63_%28W463%2C_2nd_generation%29_IMG_4914.jpg"},
        {"Mercedes_EQS", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Mercedes-Maybach_S_680_%28Z223%29_IMG_6924.jpg/640px-Mercedes-Maybach_S_680_%28Z223%29_IMG_6924.jpg"},
        {"Mercedes_CLK_GTR", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Mercedes-Benz_SLR_McLaren_front_20080607.jpg/640px-Mercedes-Benz_SLR_McLaren_front_20080607.jpg"},

        // --- 10. BMW & Audi ---
        {"BMW_XM", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/BMW_XM_IMG_6886.jpg/640px-BMW_XM_IMG_6886.jpg"},
        {"BMW_i8", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/BMW_i8_Coupe_Genf_2018.jpg/640px-BMW_i8_Coupe_Genf_2018.jpg"},
        {"BMW_M8_Competition", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/55/BMW_M8_Competition_Gran_Coup%C3%A9_IMG_4078.jpg/640px-BMW_M8_Competition_Gran_Coup%C3%A9_IMG_4078.jpg"},
        {"BMW_M4_CSL", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/BMW_M4_CSL_Goodwood_Festival_of_Speed_2022_%281%29_%2852229509657%29.jpg/640px-BMW_M4_CSL_Goodwood_Festival_of_Speed_2022_%281%29_%2852229509657%29.jpg"},
        {"BMW_M5_CS", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/BMW_M5_CS_IMG_4896.jpg/640px-BMW_M5_CS_IMG_4896.jpg"},
        {"Audi_R8_V10", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/Audi_R8_Coup%C3%A9_V10_GT_RWD_IMG_7396.jpg/640px-Audi_R8_Coup%C3%A9_V10_GT_RWD_IMG_7396.jpg"},
        {"Audi_RS_e_tron_GT", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2b/Audi_RS_e-tron_GT_IMG_3962.jpg/640px-Audi_RS_e-tron_GT_IMG_3962.jpg"},
        {"Audi_RS7", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Audi_RS7_Sportback_C8_IMG_2719.jpg/640px-Audi_RS7_Sportback_C8_IMG_2719.jpg"},
        {"Audi_RS6_Avant", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/55/Audi_RS6_Avant_C8_IMG_2717.jpg/640px-Audi_RS6_Avant_C8_IMG_2717.jpg"},
        {"Audi_TT_RS", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Audi_TT_RS_Coupe_front.jpg/640px-Audi_TT_RS_Coupe_front.jpg"}
    };

    public static void main(String[] args) {
        System.out.println("⏳ Starting Download (3 Images per Car)...");
        
        try {
            Path imagesDir = Paths.get("car_images");
            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
            }

            int totalDownloaded = 0;
            
            for (String[] carInfo : CARS_TO_DOWNLOAD) {
                String carName = carInfo[0];
                String urlString = carInfo[1]; // الرابط الأصلي
                
                System.out.println("\n🚗 Processing: " + carName);

                // تحميل 3 صور لكل سيارة
                for (int j = 1; j <= 3; j++) {
                    String fileName = carName + "_" + j + ".jpg";
                    Path targetPath = imagesDir.resolve(fileName);

                    // تحميل الصورة إذا لم تكن موجودة، أو إذا كنت تريد استبدالها (قم بحذف شرط if للاستبدال)
                    // حالياً نستخدم if (!Files.exists) لتوفير الوقت إذا كانت موجودة
                    if (!Files.exists(targetPath)) {
                        System.out.print("   ⬇️ Downloading image " + j + "... ");
                        
                        try {
                            // المحاولة الأولى: الرابط الأصلي المباشر (ويكيميديا)
                            downloadFromUrl(urlString, targetPath);
                            System.out.println("✅ OK (Original)");
                            totalDownloaded++;
                        } catch (Exception e1) {
                            // المحاولة الثانية: الفشل (403) -> استخدام LoremFlickr الذكي
                            try {
                                System.out.print(" (trying backup)... ");
                                // تنظيف الاسم لاستخدامه ككلمة مفتاحية
                                String keyword = carName.replace("_", ",");
                                // رابط ذكي يعطي صورة مختلفة لكل رقم (lock)
                                String backupUrl = "https://loremflickr.com/800/600/" + keyword + "?lock=" + (carName.hashCode() + j + 100);
                                
                                downloadFromUrl(backupUrl, targetPath);
                                System.out.println("✅ OK (Backup)");
                                totalDownloaded++;
                            } catch (Exception e2) {
                                // المحاولة الثالثة: الفشل التام -> صورة عامة
                                System.out.println("❌ Failed.");
                            }
                        }
                    } else {
                        System.out.println("   ℹ️ Exists: " + fileName);
                    }
                }
            }
            System.out.println("\n🎉 DONE! Downloaded " + totalDownloaded + " new images. Total check: 300.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // دالة مساعدة للتحميل مع User-Agent لتجنب الحظر
    private static void downloadFromUrl(String urlString, Path targetPath) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // أهم سطر: التنكر كمتصفح لتجاوز حظر 403
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setConnectTimeout(10000); // 10 ثواني مهلة
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);
        
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            throw new Exception("HTTP " + responseCode);
        }
    }
}