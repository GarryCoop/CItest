<?php
class Login extends CI_Controller {

	public function __construct()
	{
		parent::__construct();
                date_default_timezone_set('Asia/Singapore');
		$this->load->model('admintool/login_model');
	}

	public function index()
        {
            log_message('debug','TRACE :: login.php :: index');
                $this->load->helper('form');
                $this->load->library('form_validation');

                $data['title'] = 'Admin Tool Login';

                $this->form_validation->set_rules('name', 'name', 'required');
                $this->form_validation->set_rules('pwd', 'pwd', 'required');

                if ($this->form_validation->run() === FALSE)
                {
                    log_message('debug','TRACE :: login.php :: index :: FALSE');
                        $this->load->view('templates/header', $data);
                        $this->load->view('admintool/index');
                        $this->load->view('templates/footer');

                }
                else
                {
                    log_message('debug','TRACE :: login.php :: index :: TRUE');
                        $result = $this->login_model->check_login($this->input->post('name'));
                        if($result == null){
                            log_message('error','No data found for this name:'.$this->input->post('name'));
                            $this->load->view('admintool/index');
                        }else{
//                            log_message('error','res:'.  print_r($res,1));
                            if($result['userA_pwd'] != $this->input->post('pwd')){
                                return $this->load->view('admintool/index');
                            }else{
                                return $this->load->view('admintool/success');
                            }
                            return $this->load->view('admintool/index');
                        }
                }
        }

	public function view()
        {
            log_message('debug','TRACE :: login.php :: view');
                $this->load->helper('form');
                $this->load->library('form_validation');

                $data['title'] = 'Admin Tool Login';

                $this->form_validation->set_rules('name', 'name', 'required');
                $this->form_validation->set_rules('pwd', 'pwd', 'required');

                if ($this->form_validation->run() === FALSE)
                {
                    log_message('debug','TRACE :: login.php :: view :: FALSE');
                        $this->load->view('templates/header', $data);
                        $this->load->view('admintool/index');
                        $this->load->view('templates/footer');

                }
                else
                {
                    log_message('debug','TRACE :: login.php :: view :: TRUE');
                        $result = $this->login_model->check_login($this->input->post('name'));
                        if($result == null){
                            log_message('error','No data found for this name:'.$this->input->post('name'));
                            $this->load->view('admintool/index');
                        }else{
                            foreach($result as $res){
                                if($res['userA_pwd'] != $this->input->post('pwd')){
                                    $this->load->view('admintool/index');
                                }else{
                                    $this->load->view('admintool/success');
                                }
                            }
                            $this->load->view('admintool/index');
                        }
                }
        }
}