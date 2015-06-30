<?php
class Login_model extends CI_Model {

	public function __construct()
	{
		$this->load->database();
	}
        
         // Code Sample
//        public function get_login($slug = FALSE)
//        {
//                if ($slug === FALSE)
//                {
//                        $query = $this->db->get('news');
//                        return $query->result_array();
//                }
//
//                $query = $this->db->get_where('news', array('slug' => $slug));
//                return $query->row_array();
//        }
        
        // Check Login with name
        public function check_login($name)
        {
//                $this->load->helper('url');
                if($name != null && $name != ''){
                    $query = $this->db->get_where('userA', array('userA_name' => $name));
//                    log_message('info','res--:: row_array'.  print_r($query->row_array(),1));
//                    log_message('info','res--:: result_array'.  print_r($query->result_array,1));
//                    log_message('info','res--:: result_object'.  print_r($query->result_object,1));
//                    log_message('info','res--:: custom_ersult_object'.  print_r($query->custom_result_object,1));
                    return $query->row_array();
                }else{
                    return $null;
                }
        }
        
        // Check Login with name and password
        public function check_login_NamePwd($name, $pwd)
        {
//                $this->load->helper('url');
                if($name != null && $name != '' && $pwd != null & $pwd != ''){
                    $query = $this->db->get_where('userA', array('userA_name' => $name, 'userA_pwd' => $pwd));
//                    log_message('info','res--:: row_array'.  print_r($query->row_array(),1));
//                    log_message('info','res--:: result_array'.  print_r($query->result_array,1));
//                    log_message('info','res--:: result_object'.  print_r($query->result_object,1));
//                    log_message('info','res--:: custom_ersult_object'.  print_r($query->custom_result_object,1));
                    return $query->row_array();
                }else{
                    return $null;
                }
        }
}

