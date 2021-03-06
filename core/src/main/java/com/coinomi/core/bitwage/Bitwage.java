package com.coinomi.core.bitwage;

import com.coinomi.core.bitwage.data.employer.payrolls.PayrollCreation;
import com.coinomi.core.bitwage.data.employer.payrolls.WorkerPayrolls;
import com.coinomi.core.bitwage.data.employer.invoices.CompanyInvoices;
import com.coinomi.core.bitwage.data.employer.invoices.Invoice;
import com.coinomi.core.bitwage.data.employer.invoices.InvoiceApproval;
import com.coinomi.core.bitwage.data.employer.payrolls.CompanyPaymentMethod;
import com.coinomi.core.bitwage.data.employer.payrolls.CompanyPayroll;
import com.coinomi.core.bitwage.data.user.Companies;
import com.coinomi.core.bitwage.data.employer.profile.Company;
import com.coinomi.core.bitwage.data.employer.payrolls.CompanyPayrollInfo;
import com.coinomi.core.bitwage.data.employer.workers.EmailToIdResults;
import com.coinomi.core.bitwage.data.user.Employer;
import com.coinomi.core.bitwage.data.employer.workers.Invite;
import com.coinomi.core.bitwage.data.employer.profile.LinkedAccount;
import com.coinomi.core.bitwage.data.log.DeletePayrollLog;
import com.coinomi.core.bitwage.data.log.InviteLog;
import com.coinomi.core.bitwage.data.worker.UserPayrollsInfo;
import com.coinomi.core.bitwage.data.user.Profile;
import com.coinomi.core.bitwage.data.employer.workers.WorkersSimple;
import com.coinomi.core.bitwage.data.PaymentMethod;
import com.coinomi.core.bitwage.data.Tickers;
import com.coinomi.core.bitwage.data.UserKeyPair;
import com.coinomi.core.bitwage.data.employer.workers.Worker;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.coinomi.core.bitwage.Constants.ACCESS_KEY;
import static com.coinomi.core.bitwage.Constants.ACCESS_NONCE;
import static com.coinomi.core.bitwage.Constants.ACCESS_SIGNATURE;
import static com.coinomi.core.bitwage.Constants.MEDIA_TYPE_JSON;
import static com.coinomi.core.bitwage.Constants.THIS_USER_AGENT;
import static com.coinomi.core.bitwage.Constants.USER_APP;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.USER_AGENT;

/**
 * @author gkoro
 */

public class Bitwage extends Connection {
    private static final Logger log = LoggerFactory.getLogger(Bitwage.class);

    private static final String GET_TICKERS = "tickers";
    private static final String GET_TICKER = "ticker/%s";

    /*User:Profile*/
    private static final String GET_PROFILE = "company/userid";
    private static final String GET_COMPANIES = "company/companies";
    private static final String BPI_COMPANY_VIEW = "user/bpi_company/view";
    private static final String BPI_COMPANY_EDIT = "user/bpi_company/edit";
    private static final String BPI_COMPANY_ADD = "user/bpi_company/add";
    private static final String BPI_COMPANY_DELETE = "user/bpi_company/delete";

    /*Worker:History*/
    private static final String USER_PAYROLLS = "user/payrolls";

    /*Employer:Profile*/
    private static final String GET_COMPANY_BY_ID = "company?company_id=%s";
    private static final String GET_LINKED_ACCOUNTS = "company/linkedaccounts?company_id=%s";

    /*Employer: Workers*/
    private static final String GET_WORKERS = "company/workers?company_id=%s&page=%d";
    private static final String GET_WORKER_BY_ID= "company/worker?company_id=%s&user_id=%s";
    private static final String INVITE_WORKER = "company/workers/invite?company_id=%s";
    private static final String EMAIL_TO_ID = "company/workers/emailtoid?company_id=%s";

    /*Employer:Payrolls*/
    private static final String GET_COMPANY_PAYROLLS = "company/payrolls?company_id=%s&page=%d";
    private static final String GET_COMPANY_PAYROLL_BY_ID = "company/payroll?company_id=%s&payroll_id=%s&page=%d";
    private static final String GET_WORKER_PAYROLLS = "company/worker/payrolls?company_id=%s&user_id=%s&page=%d";
    private static final String CREATE_PAYROLL = "company/workers/pay?company_id=%s&paywithid=%b&deleteifnotmethod=%b";
    private static final String SET_PAYROLL_METHOD = "company/payroll/method?company_id=%s&payroll_id=%s";
    private static final String DELETE_PAYROLL = "company/payroll/delete";

    /*Employer:Invoices*/
    private static final String GET_COMPANY_INVOICES = "company/invoices?company_id=%s";
    private static final String GET_COMPANY_INVOICE = "company/invoices?company_id=%s&invoice_id=%s";
    private static final String APPROVE_INVOICE = "company/invoice/approve?company_id=%s";

    private String apiKey;
    private String secretKey;

    public Bitwage(OkHttpClient client, UserKeyPair userkeypair) {
        super(client);
        this.apiKey = userkeypair.getApikey();
        this.secretKey = userkeypair.getApisecret();
    }

    public Bitwage(UserKeyPair userkeypair) {
        this.apiKey = userkeypair.getApikey();
        this.secretKey = userkeypair.getApisecret();
    }

    public Tickers getTickers() throws ShapeShiftException, IOException {
        Request request = new Request.Builder().url(getApiUrl(GET_TICKERS)).build();
        return new Tickers(getMakeApiCall(request));
    }

    public Tickers getTicker(String currency) throws ShapeShiftException, IOException {
        Request request = new Request.Builder().url(getApiUrl(String.format(GET_TICKER,currency))).build();
        return new Tickers(getMakeApiCall(request));
    }

    public Profile getProfile() throws ShapeShiftException, IOException {
        Request request = buildGetRequest(getApiUrl(GET_PROFILE));
        return new Profile(getMakeApiCall(request));
    }

    public Companies getCompanies() throws ShapeShiftException, IOException {
        String accessnonce = String.valueOf(System.currentTimeMillis());
        Request request = buildGetRequest(getApiUrl(GET_COMPANIES));
        return new Companies(getMakeApiCall(request));
    }

    public List<Employer> bpicompanyview() throws ShapeShiftException, IOException, JSONException {
        List<Employer> employerlist = new ArrayList<>();
        Request request = buildGetRequest(getApiUrl(BPI_COMPANY_VIEW));
        JSONArray employersarray = getMakeApiCall(request).getJSONArray("bpiemplist");
        for (int i=0 ; i < employersarray.length() ; i++) {
            employerlist.add(new Employer(employersarray.getJSONObject(i)));
        }
        return employerlist;
    }

    public List<Employer> bpicompanyedit(Employer employer) throws JSONException, IOException, ShapeShiftException {

        List<Employer> employerlist = new ArrayList<>();
        JSONObject jsontopost = employer.getJson();
        jsontopost.remove("order");

        Request request = buildPostRequest(getApiUrl(BPI_COMPANY_EDIT), jsontopost);
        JSONArray employersarray = getMakeApiCall(request).getJSONArray("bpiemplist");
        for (int i=0 ; i < employersarray.length() ; i++) {
            employerlist.add(new Employer(employersarray.getJSONObject(i)));
        }
        return employerlist;
    }
    //TODO: add unit test
    public List<Employer> bpicompanyadd(Employer employer) throws JSONException, IOException, ShapeShiftException {

        List<Employer> employerlist = new ArrayList<>();

        JSONObject jsontopost = employer.getJson();
        jsontopost.remove("order");

        Request request = buildPostRequest(getApiUrl(BPI_COMPANY_ADD), jsontopost);
        JSONArray employersarray = getMakeApiCall(request).getJSONArray("bpiemplist");
        for (int i=0 ; i < employersarray.length() ; i++) {
            employerlist.add(new Employer(employersarray.getJSONObject(i)));
        }
        return employerlist;
    }
    //TODO: add unit test
    public boolean bpicompanydelete(Employer employer) throws JSONException, IOException, ShapeShiftException {

        JSONObject jsontopost = new JSONObject();
        jsontopost.put("bpionboardid",employer.getBpionboardid());

        Request request = buildPostRequest( getApiUrl(BPI_COMPANY_DELETE), jsontopost);
        JSONObject response = getMakeApiCall(request);
        return response.get("status").equals("success");
    }

    public UserPayrollsInfo getPayrolls() throws ShapeShiftException, IOException, JSONException {
        Request request = buildGetRequest(getApiUrl(USER_PAYROLLS));
        return new UserPayrollsInfo(getMakeApiCall(request));
    }

    public Company getCompanyByid(BigInteger id) throws IOException, ShapeShiftException {
        String url = getApiUrl(String.format(GET_COMPANY_BY_ID, id.toString()));
        Request request = buildGetRequest(url);
        return new Company(getMakeApiCall(request));
    }

    public List<LinkedAccount> getLinkedAccountsByid(BigInteger id) throws IOException, ShapeShiftException, JSONException {
        List<LinkedAccount> linkedAccountList = new ArrayList<>();
        String url = getApiUrl(String.format(GET_LINKED_ACCOUNTS, id.toString()));
        Request request = buildGetRequest(url);
        JSONArray responsearray = getMakeApiCall(request).getJSONArray("linkedaccounts");
        for (int i=0; i < responsearray.length() ; i++) {
            linkedAccountList.add(new LinkedAccount(responsearray.getJSONObject(i)));
        }
        return linkedAccountList;
    }

    public WorkersSimple getWorkersByCompanyId(BigInteger id, int page) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(GET_WORKERS, id.toString(), page));
        Request request = buildGetRequest(url);
        return new WorkersSimple(getMakeApiCall(request));
    }

    public Worker getWorkerById (BigInteger companyid, BigInteger userid) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(GET_WORKER_BY_ID, companyid.toString(), userid.toString()));
        Request request = buildGetRequest(url);
        return new Worker(getMakeApiCall(request));
    }

    public List<InviteLog> inviteWorkers(BigInteger id, List<Invite> inviteList) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(INVITE_WORKER, id.toString()));

        JSONObject jsontopost = new JSONObject();
        JSONArray inviteArray = new JSONArray();
        for (Invite invite:inviteList) {
            inviteArray.put(invite.toString());
        }
        jsontopost.put("to_invite", inviteArray);
        Request request = buildPostRequest(url, jsontopost);

        List<InviteLog> inviteLogList= new ArrayList<>();

        JSONArray responsearray = getMakeApiCall(request).getJSONArray("invite_log");
        for (int i=0; i < responsearray.length() ; i++) {
            inviteLogList.add(new InviteLog(responsearray.getJSONObject(i)));
        }
        return inviteLogList;
    }

    public EmailToIdResults emailtoId (BigInteger id, List<String> emails) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(EMAIL_TO_ID, id.toString()));

        JSONObject jsontopost = new JSONObject();
        JSONArray emailArray = new JSONArray();
        for (String email:emails) {
            emailArray.put(email);
        }
        jsontopost.put("emails", emailArray);
        Request request = buildPostRequest(url, jsontopost);

        return new EmailToIdResults(getMakeApiCall(request));
    }

    public CompanyPayrollInfo getCompanyPayrolls(BigInteger id, int page) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(GET_COMPANY_PAYROLLS, id.toString(), page));
        Request request = buildGetRequest(url);
        return new CompanyPayrollInfo(getMakeApiCall(request));
    }

    public CompanyPayroll getCompanyPayrollById(BigInteger companyid, BigInteger payrollid, int page) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(GET_COMPANY_PAYROLL_BY_ID, companyid.toString(), payrollid.toString(), page));
        Request request = buildGetRequest(url);
        return new CompanyPayroll(getMakeApiCall(request));
    }

    public WorkerPayrolls getWorkerPayrollsByUserId (BigInteger companyid, BigInteger userid, int page) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(GET_WORKER_PAYROLLS, companyid.toString(), userid.toString(), page));
        Request request = buildGetRequest(url);
        return new WorkerPayrolls(getMakeApiCall(request));
    }

    public PayrollCreation createPayroll (BigInteger id, Boolean paywithid, Boolean deleteifnotmethod, Map<String, Double> payments) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(CREATE_PAYROLL, id.toString(), paywithid, deleteifnotmethod));

        JSONObject jsontopost = new JSONObject();
        JSONArray paymentsArray = new JSONArray();

        String receiverKey="email";
        if (paywithid) {
            receiverKey="id";
        }
        Iterator itr = payments.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry pair = (Map.Entry) itr.next();
            paymentsArray.put(new JSONObject().put(receiverKey,pair.getKey()).put("amound_usd",pair.getValue()));
        }
        jsontopost.put("to_pay", paymentsArray);

        Request request = buildPostRequest(url, jsontopost);

        return new PayrollCreation(getMakeApiCall(request));
    }
    
    public CompanyPaymentMethod setPaymentMethodForPayroll(BigInteger companyid, BigInteger payrollid, PaymentMethod paymentmethod) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(SET_PAYROLL_METHOD, companyid.toString(), payrollid.toString()));

        JSONObject jsontopost = new JSONObject();
        jsontopost.put("payment_method", paymentmethod.toString().toLowerCase());

        Request request = buildPostRequest(url, jsontopost);

        return new CompanyPaymentMethod(getMakeApiCall(request));
    }

	public DeletePayrollLog deletePayrollById(BigInteger payroll_id) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(DELETE_PAYROLL);
        JSONObject jsontopost = new JSONObject();
        jsontopost.put("to_delete", new JSONArray().put(payroll_id.toString()));
        Request request = buildPostRequest(url, jsontopost);
        return new DeletePayrollLog(getMakeApiCall(request));
	}

    public CompanyInvoices getCompanyInvoices(BigInteger companyId) throws IOException, ShapeShiftException {
        String url = getApiUrl(String.format(GET_COMPANY_INVOICES, companyId.toString()));
        Request request = buildGetRequest(url);
        return new CompanyInvoices(getMakeApiCall(request));
	}


    public Invoice getCompanyInvoiceById(BigInteger company_id, BigInteger invoice_id) throws IOException, ShapeShiftException {
		String url = getApiUrl(String.format(GET_COMPANY_INVOICE, company_id.toString(), invoice_id.toString()));
        Request request = buildGetRequest(url);
        return new Invoice(getMakeApiCall(request));
	}

	public InvoiceApproval approveInvoice(BigInteger company_id, BigInteger invoice_id) throws IOException, ShapeShiftException, JSONException {
        String url = getApiUrl(String.format(APPROVE_INVOICE, company_id.toString()));
        JSONObject jsontopost = new JSONObject();
        jsontopost.put("invoice_id",invoice_id.toString());
        Request request = buildPostRequest(url,jsontopost);
        return new InvoiceApproval(getMakeApiCall(request));
	}

    private Request buildGetRequest(String url) {
        String accessnonce = String.valueOf(System.currentTimeMillis());
        return new Request.Builder().url(url).addHeader(USER_AGENT, THIS_USER_AGENT)
                .addHeader(USER_APP, String.valueOf(true)).addHeader(ACCESS_KEY, this.apiKey)
                .addHeader(ACCESS_SIGNATURE, getHMAC256Signature(accessnonce+url, this.secretKey))
                .addHeader(CONTENT_TYPE,MEDIA_TYPE_JSON.toString()).addHeader(ACCESS_NONCE, accessnonce).build();
    }

    private Request buildPostRequest(String url, JSONObject jsontopost) {
        RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, jsontopost.toString() );
        String accessnonce = String.valueOf(System.currentTimeMillis());
        return new Request.Builder().url(url).addHeader(USER_AGENT, THIS_USER_AGENT)
                .addHeader(USER_APP, String.valueOf(true)).addHeader(ACCESS_KEY, this.apiKey)
                .addHeader(ACCESS_SIGNATURE, getHMAC256Signature(accessnonce+url+jsontopost.toString(), this.secretKey))
                .addHeader(CONTENT_TYPE,MEDIA_TYPE_JSON.toString()).addHeader(ACCESS_NONCE, accessnonce).post(body).build();
    }
    
    //Todo: Take in mind Optional Objects in JSON responses
 }
