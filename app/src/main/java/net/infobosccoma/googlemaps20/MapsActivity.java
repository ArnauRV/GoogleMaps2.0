package net.infobosccoma.googlemaps20;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends ActionBarActivity implements LocationListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;

    private static final String URL_DATA = "http://www.infobosccoma.net/pmdm/pois.php";

    private ArrayList<PuntsMapa> llistaPosicions;
    private ArrayList<LatLng> llistaPuntsPersonalitzats;
    private ArrayList<LatLng> puntsRuta;
    private List<DrawerItems> dataList;

    private LocationManager locationManager;

    private LatLng posUsuari;
    private LatLng posMarker;

    private PolylineOptions lineOptions;
    private Polyline liniaRuta;

    private DrawerLayout mDrawerLayout;
    private PersonalDrawerAdapter adapter;
    private ListView mDrawerList;

    private ActionBarDrawerToggle drawerToggle;

    private SearchView searchView;

    private ProgressBar bar;

    private boolean localitzacio, gps;
    private int vistaMapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        this.localitzacio = false;
        this.gps = false;

        bar = (ProgressBar) this.findViewById(R.id.progressBar);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Mètode que comprova si hi ha dades a recuperar del savedInstanceState
        carregarSavedInstance(savedInstanceState);

        // Mètode que instancia el mapa
        setUpMapIfNeeded();

        // Mètode que carregarà les dades del slider lateral
        carregaSliderLateral();

        // Utilitzo el botó home
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Instancio el slider lateral
        drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                 R.string.obrirSlider, R.string.tancarSlider);
        mDrawerLayout.setDrawerListener(drawerToggle);
    }

    /**
     * Mètode que comprova si hi ha dades a carregar quan
     * es gira la pantalla del mòbil.
     * @param savedInstanceState
     */
    private void carregarSavedInstance(Bundle savedInstanceState){
        if(savedInstanceState != null){
            if (savedInstanceState.containsKey("PosicionsMapa")) {
                llistaPosicions = savedInstanceState.getParcelableArrayList("PosicionsMapa");
            }

            if (savedInstanceState.containsKey("RutaEntrePunts")) {
                puntsRuta = savedInstanceState.getParcelableArrayList("RutaEntrePunts");
            }

            if (savedInstanceState.containsKey("PosicionsPersonalitzades")) {
                llistaPuntsPersonalitzats = savedInstanceState.getParcelableArrayList("PosicionsPersonalitzades");
            }else {
                llistaPuntsPersonalitzats = new ArrayList<LatLng>();
            }

            if (savedInstanceState.containsKey("Localitzacio")){
                localitzacio = savedInstanceState.getBoolean("Localitzacio");
            }

            if (savedInstanceState.containsKey("GPS")){
                gps = savedInstanceState.getBoolean("GPS");
            }

            if (savedInstanceState.containsKey("VistaMapa")){
                vistaMapa = savedInstanceState.getInt("VistaMapa");
            }

        }else{
            llistaPuntsPersonalitzats = new ArrayList<LatLng>();
        }
    }

    /**
     * Mètode que carrega les el contingut a l'slider lateral.
     */
    private void carregaSliderLateral(){
        dataList = new ArrayList<DrawerItems>();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        dataList.add(new DrawerItems("Vistes Mapa")); // afegeixo el títol de la secció
        dataList.add(new DrawerItems("Normal", R.drawable.ic_mapa_normal));
        dataList.add(new DrawerItems("Híbrid", R.drawable.ic_satelit));
        dataList.add(new DrawerItems("Topogràfic", R.drawable.ic_topografic));
        dataList.add(new DrawerItems("Satèl·lit", R.drawable.ic_satelit2));

        dataList.add(new DrawerItems("Localització")); // afegeixo el títol de la secció

        // Si hi ha la localització oberta faig que surti l'opció de desactivar
        if (localitzacio) {
            dataList.add(new DrawerItems("Desactiva Posició", R.drawable.ic_localitzacio));

            // Si hi ha el gps activat faig que surti l'opció de desactivar
            if (gps){
                dataList.add(new DrawerItems("Desactiva GPS", R.drawable.ic_gps));
            }else {
                dataList.add(new DrawerItems("GPS", R.drawable.ic_gps));
            }
        }else{
            dataList.add(new DrawerItems("Posició", R.drawable.ic_localitzacio));
        }

        adapter = new PersonalDrawerAdapter(this, R.layout.personal_drawer_item,
                dataList);

        mDrawerList.setAdapter(adapter);
    }

    /**
     * Mètode que detecta quin item es fa click del slider lateral.
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (dataList.get(position).getTitle() == null) {
                itemSeleccionat(position);
            }
        }
    }

    /**
     * Mètode que segons l'item que s'ha fet click del slider,
     * fa una cosa o una altre.
     * @param position
     */
    private void itemSeleccionat(int position){
        if (position == 1){
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            vistaMapa = 1;
        }else if (position == 2){
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            vistaMapa = 4;
        }else if (position == 3){
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            vistaMapa = 3;
        }else if (position == 4){
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            vistaMapa = 2;
        }else if (position == 6){
            if (validaLocalitzacio()){
                carregaSliderLateral();
            }else{
                showAlertDialog(MapsActivity.this, "Localització",
                        "El teu dispositiu no té la localització oberta.", true);
            }
        }else if (position == 7){
            if (!gps) {
                demanarUbicacio();
            }else{
                locationManager.removeUpdates(MapsActivity.this);
                gps = false;
            }
            carregaSliderLateral();
        }
    }

    /**
     * Mètode que obre la localització si està tancada i la
     * tanca si està oberta.
     * @return true si s'ha activat la localització
     */
    private boolean validaLocalitzacio(){

        // Comprovo si el gps del mòbil està obert
        boolean gps_obert = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (gps_obert) {

            // Si no hi ha localització, l'obro
            if (!localitzacio) {
                mMap.setMyLocationEnabled(true);
                localitzacio = true;
            } else {
                locationManager.removeUpdates(MapsActivity.this);
                mMap.setMyLocationEnabled(false);
                localitzacio = false;

                // Si hi ha una ruta dibuixada, l'esborro
                if (liniaRuta != null) {
                    liniaRuta.remove();
                    puntsRuta = null;
                }

                // Si hi ha el gps obert el paro
                if (gps){
                    gps = false;
                }
            }
            return true;
        }else{
            return false;
        }
    }

    /**
     * Mètode que infla el menú i instancia el buscador
     * de les poblacions.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        // Implementació del buscador
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);

        SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextChange(String text) {
                return false;
            }

            // Quan es fa submit es tanca el teclat i
            // s'executa el thread passant-li la població entrada.
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                // Crida al mètode que tanca el teclat.
                amagarTeclat(MapsActivity.this);

                // Si el mòbil té l'internet activat es descarrega
                // es crida el thread.
                if (estasConnectat()) {
                    new DescarregarDades().execute(query);
                }
                return true;
            }
        };
        // Iniciem el listener
        searchView.setOnQueryTextListener(textChangeListener);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Mètode que detecta si s'ha fet click al botó home
     * per mostrar l'slider.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Mètode que guarda les dades de les diferents variables per
     * carregar-les de nou quan es gira la pantalla del mòbil.
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("PosicionsMapa", llistaPosicions);
        outState.putParcelableArrayList("PosicionsPersonalitzades", llistaPuntsPersonalitzats);
        outState.putParcelableArrayList("RutaEntrePunts", puntsRuta);
        outState.putBoolean("Localitzacio", localitzacio);
        outState.putBoolean("GPS", gps);
        outState.putInt("VistaMapa", vistaMapa);
        super.onSaveInstanceState(outState);
    }

    /**
     * Mètode que quan se surt de l'aplicació, però no es tanca,
     * para les peticions del gps per no gastar bateria.
     */
    @Override
    public void onPause() {
        super.onPause();
        mMap.setMyLocationEnabled(false);
        locationManager.removeUpdates(this);
    }

    /**
     * Mètode que quan es torna a l'aplicació oberta en segon pla,
     * torna activar les peticions del gps si estaven activades
     * abans de sortir de l'aplicació.
     */
    @Override
    protected void onResume() {
        super.onResume();

        setUpMapIfNeeded();

        if (localitzacio) {
            mMap.setMyLocationEnabled(true);
        }

        if (gps) {
            String providerName = getProviderName();
            locationManager.requestLocationUpdates(providerName, 500, 0, this);
        }
    }

    /**
     * Mètode que activa les peticions de gps cada mig segon
     */
    private void demanarUbicacio() {
        String providerName = getProviderName();

        if (providerName == null) {
            Toast.makeText(this,"No hi ha cap proveïdor que compleixi la cerca", Toast.LENGTH_LONG).show();
        } else {
            // Mètode que activa les peticions de gps. Cada mig segon
            // comprovarà l'ubicació.
            if (locationManager.isProviderEnabled(providerName)) {
                locationManager.requestLocationUpdates(providerName,500,0,this);
                gps = true;
            }
        }
    }

    /**
     * Mètode qeu retorna el millor proveïdor de localització
     * (gps o internet) segons els paràmetres entrats
     * @return
     */
    private String getProviderName() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);
        return locationManager.getBestProvider(criteria, true);
    }

    /**
     * Mètode que instancia el mapa
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {

                // instancio els listeners
                mMap.setOnMarkerClickListener(this);
                mMap.setOnMapLongClickListener(this);

                // Comprovo si llista de rutes no està buida
                // si no ho està, crido el mètode que la pinti.
                if (puntsRuta != null){
                    pintaRutaUsuari(puntsRuta);
                }

                // Comprovo si la llista de punts personalitzats no
                // està buida i si no ho està, crido el mètode que la pinti.
                if (llistaPuntsPersonalitzats != null){
                    pintaMarkersPersonalitzats(llistaPuntsPersonalitzats);
                }

                // Comprovo si la llista de posicions del buscador no
                // està buida i si no ho està, crido el mètode que la pinti.
                if (llistaPosicions != null){
                    setUpMap(llistaPosicions);
                }else {
                    setUpMap();
                }
            }
        }
    }

    /**
     * Mètode que pinta una ruta
     * @param puntsRuta
     */
    private void pintaRutaUsuari(ArrayList<LatLng> puntsRuta) {
        // Si hi ha una ruta pintada, l'esborro
        if (liniaRuta != null) {
            liniaRuta.remove();
        }

        // Paràmetres de la línia de la ruta
        lineOptions = new PolylineOptions();
        lineOptions.addAll(puntsRuta);
        lineOptions.width(8);
        lineOptions.color(Color.RED);

        liniaRuta = mMap.addPolyline(lineOptions);
    }

    /**
     * Mètode que repinta els markers clicats per l'usuari quan es gira la pantalla
     * @param llista
     */
    private void pintaMarkersPersonalitzats(ArrayList<LatLng> llista) {
        for (int i = 0; i < llista.size(); i++) {
            mMap.addMarker(new MarkerOptions().position(llista.get(i))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
    }

    /**
     * Mètode que comprova si hi ha un vista del mapa activada
     * quan es gira la pantalla. Si n'hi ha una d'activada, la
     * selecciona, sinó poso la vista normal per defecte
     */
    private void setUpMap() {
        if (vistaMapa != 0){
            mMap.setMapType(vistaMapa);
        }else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    /**
     * Mètode que pinta els punts trobats per el buscador al mapa
     * @param llista
     */
    private void setUpMap(ArrayList<PuntsMapa> llista) {

        // emmagatzemo la llista
        llistaPosicions = llista;

        for (int i = 0; i < llista.size(); i++) {
            mMap.addMarker(new MarkerOptions().position(new LatLng(llista.get(i).getLatitude(), llista.get(i).getLongitude())).title(llista.get(i).getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); //Pinta de color blau el marker
        }
    }

    /**
     * Mètode que fa un zoom automàtic passant-li una sèrie de punts del mapa
     */
    private void clicCentrar() {
        LatLngBounds.Builder latlngbounds = new LatLngBounds.Builder();
        for (int i = 0; i < llistaPosicions.size(); i++) {
            latlngbounds.include(new LatLng(llistaPosicions.get(i).getLatitude(), llistaPosicions.get(i).getLongitude()));
        }

        // l'integer final és el padding
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latlngbounds.build(), 275));
    }

    /**
     * Mètode que s'executa quan canvia la localització en el mapa.
     * Va actualitzant la posició (gps).
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        // Detecto la posició actual
        posUsuari = new LatLng(location.getLatitude(), location.getLongitude());

        if (gps) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(posUsuari) // Em poso al centre del mapa
                    .zoom(17) // Zoom
                    .bearing(location.getBearing()) // Orientació de la càmera. Va girant quan es va canviant de sentit
                    .build();
            // Vaig actualitzant la posició de la càmera.
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "S'ha activat el senyal d'ubicació", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * Quan faig click a un marker, crido el thread perquè em pinti
     * la ruta al mapa
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();

        boolean gps_obert = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // Comprovo si el gps del mòbil està obert
        if (gps_obert) {

            // Comprovo si l'internet del mòbil està obert
            if (estasConnectat()) {
                // Comprovo si la localització de l'aplicació està oberta
                if (localitzacio) {
                    // Valido que s'hagi carregat la nostra ubicacio al mapa
                    if (mMap.getMyLocation() != null) {
                        // Obtinc le coordenades del nou marker
                        posMarker = marker.getPosition();
                        String url = getDirectionsUrl(getPosUsuari(), posMarker);
                        DownloadTask downloadTask = new DownloadTask();

                        // Comença a descarregar la informacio de la API Google Directions
                        downloadTask.execute(url);
                        return true;
                    // Si no s'ha carregat la nostra localització al mapa, mostro un toast d'avís
                    }else{
                        Toast.makeText(this, "Esperi que es carregui la localització", Toast.LENGTH_LONG).show();
                        return false;
                    }
                // Si no hi ha la localització de l'aplicació oberta mostro un toast
                } else {
                    Toast.makeText(this, "Activa la localització al menú d'opcions", Toast.LENGTH_LONG).show();
                    return false;
                }
            // Si no hi ha l'internet del mòbil obert, crido el mètode showAlertDialog
            // que em mostra un dialog amb l'opció d'obrir les opcions d'internet del mòbil.
            }else{
                showAlertDialog(MapsActivity.this, "Connexió a Internet",
                        "El teu dispositiu no té connexió a Internet.", false);
                return false;
            }
        // Si no hi ha la localització del mòbil oberta, crido el mètode showAlertDialog
        // que em mostra un dialog amb l'opció d'obrir les opcions de localització del mòbil.
        }else{
            showAlertDialog(MapsActivity.this, "Localització",
                    "El teu dispositiu no té la localitzacio oberta.", true);
            return false;
        }
    }

    /**
     * Mètode que retorna les coordenades de l'usuari
     * @return les coordenades de l'usuari
     */
    private LatLng getPosUsuari(){
        return new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
    }

    /**
     * Mètode que quan es fa un longClick al mapa, es crea un nou marker
     * @param punt
     */
    @Override
    public void onMapLongClick(LatLng punt) {

        // Afegeixo el nou punt a la llista de punts personalitzats
        llistaPuntsPersonalitzats.add(punt);

        // Opcions del marker
        MarkerOptions options = new MarkerOptions();
        options.position(punt);
        options.title(punt.longitude + "  " + punt.latitude);
        // Marker personalitzat de color verd
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        // Pinto el marker al mapa
        mMap.addMarker(options);
    }

    /**
     * Mètode que valida si hi ha l'internet del mòbil activat (dades o wi-fi)
     * @return true si hi internet. Fals si està parat
     */
    protected Boolean estasConnectat(){

        // Comprovo si hi ha el wifi obert
        if(connectatWifi()){
            return true;
        }else{
            // Sinó comprovo si hi ha les dades del mòbil obertes
            if(connectatDadesMobil()){
                return true;
            // Si no hi ha l'internet obert, retorno fals
            }else{
                return false;
            }
        }
    }

    /**
     * Mètode que comprova si hi ha el wi-fi obert
     * @return true si està obert, fals si està tancat
     */
    protected Boolean connectatWifi(){
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (info != null) {
                if (info.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Mètode que comprova si hi ha les dades del mòbil obertes.
     * @return true si està activat, fals si està tancat
     */
    protected Boolean connectatDadesMobil(){
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (info != null) {
                if (info.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Mètode que mostra un diàlog amb les opcions corresponents segons el tipus.
     * Si el tipus és true es mostra l'opció d'obrir les opcions de localització.
     * Si el tipus és false es mostra l'opció d'obrir les opcions d'internet
     * @param context
     * @param title
     * @param message
     * @param tipus
     */
    public void showAlertDialog(Context context, String title, String message, Boolean tipus) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);

        if (!tipus) {
            alertDialog.setButton(-1, "Obre les opcions d'internet", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                }
            });
        }else{
            alertDialog.setButton(-1, "Obre les opcions de localització", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
        }

        alertDialog.setButton(-2,"Cancel·lar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.closeOptionsMenu();
            }
        });

        alertDialog.show();
    }

    /**
     * Mètode que amaga el teclat un cop es fa la búsqueda d'una població
     * @param activity
     */
    public static void amagarTeclat(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    /**
     * Classe AsyncTask que descarrega els punts de la població entrada al buscador
     */
    class DescarregarDades extends AsyncTask<String, Void, ArrayList<PuntsMapa>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Mostro la progressbar
            bar.setVisibility(View.VISIBLE);
            // Netejo el mapa
            mMap.clear();
        }

        /**
         * Procés de descarrega dels punts
         * @param params
         * @return la llista de punts
         */
        @Override
        protected ArrayList<PuntsMapa> doInBackground(String... params) {
            ArrayList<PuntsMapa> llistaPunts = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httppostreq = new HttpPost(URL_DATA);
            HttpResponse httpresponse = null;
            try {
                List<NameValuePair> parametres = new ArrayList<NameValuePair>(1);
                // Com a paràmetre li dic que només descarregui els punts de les ciutats
                // que tinguin el valor entrat al buscador
                parametres.add(new BasicNameValuePair("city", params[0]));
                httppostreq.setEntity(new UrlEncodedFormEntity(parametres));
                httpresponse = httpclient.execute(httppostreq);
                String responseText = EntityUtils.toString(httpresponse.getEntity());
                // Retorno la llista de punts
                llistaPunts = tractarJSON(responseText);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return llistaPunts;
        }

        /**
         * Després de descarregar-se els punts, faig que els pinti cridant el
         * mètode setUpMap o bé mostro un toast si no s'ha trobat cap punt
         * @param llista
         */
        @Override
        protected void onPostExecute(ArrayList<PuntsMapa> llista) {
            if (!llista.isEmpty()) {
                // Pinta els punts al mapa
                setUpMap(llista);
                // Fa el zoom del punts
                clicCentrar();
            }else{
                Toast toast = Toast.makeText(MapsActivity.this, "No hi ha cap coincidència", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 200);
                toast.show();
            }
            // Amago la progressbar
            bar.setVisibility(View.GONE);
        }

        /**
         * Mètode que descarrega els punts del servidor
         * @param json
         * @return llista de punts
         */
        private ArrayList<PuntsMapa> tractarJSON(String json) {
            Gson converter = new Gson();
            return converter.fromJson(json, new TypeToken<ArrayList<PuntsMapa>>(){}.getType());
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected void onPreExecute(){
            bar.setVisibility(View.VISIBLE);
        }
        // Descàrrega dels punts
        @Override
        protected String doInBackground(String... url) {

            // Per a l'emmagatzematge de dades de servei web
            String data = "";

            try{
                // Extraient les dades de servei web
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invocació del fil per analitzar les dades JSON
            parserTask.execute(result);
        }
    }

    /**
     * Classe per analitzar les rutes de Google en format JSON
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            puntsRuta = null;

            // Recorrent a través de totes les rutess
            for(int i=0;i<result.size();i++){
                puntsRuta = new ArrayList<LatLng>();

                List<HashMap<String, String>> path = result.get(i);

                // Obtenció de tots els punts de la ruta
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    // Vaig afegint els punts a la ruta
                    puntsRuta.add(position);
                }

                // Amago la progressbar
                bar.setVisibility(View.GONE);

                // Pinto els punts de la ruta
                pintaRutaUsuari(puntsRuta);
            }
        }
    }

    /**
     * Mètode per descarregar dades JSON de una url
     * @param strUrl
     * @return
     * @throws IOException
     */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creació d'una connexió HTTP per comunicar-se amb la url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connexió amb la url
            urlConnection.connect();

            // llegint les dades de la url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.e("Err descarrega url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /**
     * Mètode que construeix l'url per descarregar una ruta de google
     * @param origin
     * @param dest
     * @return retorno la url (string) de descàrrega
     */
    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // origen de la ruta
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // destí de la ruta
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // construcció dels paràmetres per al web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // format de sortida
        String output = "json";

        // construcció de la URL al servei web
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }
}